data "aws_caller_identity" "current" {}

# ---------------------------------------------------------------------------
# ALBアクセスログ保存用S3バケット
# 障害調査やWAF誤検知の裏付けにアクセスログが必要なため出力する
# ---------------------------------------------------------------------------
resource "aws_s3_bucket" "access_logs" {
  bucket        = "${var.project}-${var.env}-alb-logs"
  force_destroy = true

  tags = {
    Name    = "${var.project}-${var.env}-alb-logs"
    Project = var.project
    Env     = var.env
  }
}

resource "aws_s3_bucket_public_access_block" "access_logs" {
  bucket = aws_s3_bucket.access_logs.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "access_logs" {
  bucket = aws_s3_bucket.access_logs.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_versioning" "access_logs" {
  bucket = aws_s3_bucket.access_logs.id
  versioning_configuration {
    status = "Enabled"
  }
}

# コスト削減のため、一定期間経過後にIA/Glacierへ移行し、最終的に自動削除する
resource "aws_s3_bucket_lifecycle_configuration" "access_logs" {
  bucket = aws_s3_bucket.access_logs.id

  rule {
    id     = "expire-alb-logs"
    status = "Enabled"

    filter {}

    transition {
      days          = 30
      storage_class = "STANDARD_IA"
    }

    transition {
      days          = 90
      storage_class = "GLACIER"
    }

    expiration {
      days = var.access_log_expiration_days
    }

    # versioning有効化に伴い、旧バージョンも同様に整理する
    noncurrent_version_expiration {
      noncurrent_days = var.access_log_expiration_days
    }

    abort_incomplete_multipart_upload {
      days_after_initiation = 7
    }
  }
}

# 全リージョン共通で使えるELBログ配信サービスプリンシパル方式（リージョンごとのELBアカウントID一覧が不要）
resource "aws_s3_bucket_policy" "access_logs" {
  bucket = aws_s3_bucket.access_logs.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "AWSLogDeliveryWrite"
        Effect    = "Allow"
        Principal = { Service = "delivery.logs.amazonaws.com" }
        Action    = "s3:PutObject"
        Resource  = "${aws_s3_bucket.access_logs.arn}/${var.project}-${var.env}-alb/AWSLogs/${data.aws_caller_identity.current.account_id}/*"
        Condition = {
          StringEquals = {
            "s3:x-amz-acl"      = "bucket-owner-full-control"
            "aws:SourceAccount" = data.aws_caller_identity.current.account_id
          }
        }
      },
      {
        Sid       = "AWSLogDeliveryAclCheck"
        Effect    = "Allow"
        Principal = { Service = "delivery.logs.amazonaws.com" }
        Action    = "s3:GetBucketAcl"
        Resource  = aws_s3_bucket.access_logs.arn
        Condition = {
          StringEquals = {
            "aws:SourceAccount" = data.aws_caller_identity.current.account_id
          }
        }
      }
    ]
  })
}

resource "aws_lb" "this" {
  name               = "${var.project}-${var.env}-alb"
  internal           = false
  load_balancer_type = "application"
  subnets            = var.public_subnet_ids
  security_groups    = [aws_security_group.alb.id]

  enable_deletion_protection = var.enable_deletion_protection

  # 不正なHTTPヘッダを除去し、HTTPデシンク（リクエストスマグリング）攻撃を緩和する
  drop_invalid_header_fields = true
  desync_mitigation_mode     = "defensive"

  # アプリ側のkeepaliveタイムアウトはこの値より長く設定すること
  idle_timeout = var.idle_timeout

  # WAF障害時にリクエストを遮断(fail closed)するか通す(fail open)か
  enable_waf_fail_open = var.enable_waf_fail_open

  access_logs {
    bucket  = aws_s3_bucket.access_logs.id
    prefix  = "${var.project}-${var.env}-alb"
    enabled = true
  }

  tags = {
    Name = "${var.project}-${var.env}-alb"
  }

  depends_on = [aws_s3_bucket_policy.access_logs]
}

resource "aws_security_group" "alb" {
  name   = "${var.project}-${var.env}-alb-sg"
  vpc_id = var.vpc_id

  # このSGのルールは全て aws_vpc_security_group_ingress_rule / aws_vpc_security_group_egress_rule
  # （別リソース）で定義する。インラインのingress/egressブロックとは絶対に混在させないこと。
  # 混在させると、Terraformの既知の不具合によりルールの競合・永続的なdiff（apply後も
  # 差分が消えない状態）が発生する。
  # （このリソースにingress/egressブロックが無い場合、TerraformはこのSGに対する
  #   デフォルトのアウトバウンド全許可ルールを削除し、明示的に許可したもの以外は
  #   全方向拒否の状態で作成する）

  tags = {
    Name = "${var.project}-${var.env}-alb-sg"
  }
}

resource "aws_vpc_security_group_ingress_rule" "alb_http" {
  security_group_id = aws_security_group.alb.id
  description       = "Allow HTTP from internet (redirected to HTTPS)"
  from_port         = 80
  to_port           = 80
  ip_protocol       = "tcp"
  cidr_ipv4         = "0.0.0.0/0"
}

resource "aws_vpc_security_group_ingress_rule" "alb_https" {
  security_group_id = aws_security_group.alb.id
  description       = "Allow HTTPS from internet"
  from_port         = 443
  to_port           = 443
  ip_protocol       = "tcp"
  cidr_ipv4         = "0.0.0.0/0"
}

# アウトバウンド(ECSタスクのアプリポートへの許可)は module.ecs との循環モジュール参照を
# 避けるため envs/dev/main.tf 側で aws_vpc_security_group_egress_rule として定義する。

resource "aws_lb_target_group" "this" {
  name        = "${var.project}-${var.env}-tg"
  port        = var.target_port
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"

  # デフォルト300秒は長くローリングデプロイを遅らせるため短縮。
  # ECSタスク定義のstopTimeout・アプリのグレースフルシャットダウン時間との整合を取ること。
  deregistration_delay = var.deregistration_delay

  health_check {
    enabled             = true
    path                = var.health_check_path
    protocol            = "HTTP"
    matcher             = "200"
    interval            = 30
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  tags = {
    Name = "${var.project}-${var.env}-tg"
    Project = var.project
    Env     = var.env
  }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.this.arn
  port              = 80
  protocol          = "HTTP"

  # HTTPは常にHTTPSへリダイレクトする（メンテナンス応答はHTTPS側で返す）
  default_action {
    type = "redirect"

    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }
}

resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.this.arn
  port              = 443
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn   = aws_acm_certificate.self_signed.arn

  default_action {
    type             = var.maintenance_mode_enabled ? "fixed-response" : "forward"
    target_group_arn = var.maintenance_mode_enabled ? null : aws_lb_target_group.this.arn

    dynamic "fixed_response" {
      for_each = var.maintenance_mode_enabled ? [1] : []
      content {
        content_type = "application/json"
        message_body = "{\"status\":503,\"error\":\"Service Unavailable\",\"message\":\"ただいまメンテナンス中です\"}"
        status_code  = "503"
      }
    }
  }
}
