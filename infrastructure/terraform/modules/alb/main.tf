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
  enable_deletion_protection = false

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

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project}-${var.env}-alb-sg"
  }
}

resource "aws_lb_target_group" "this" {
  name        = "${var.project}-${var.env}-tg"
  port        = var.target_port
  protocol    = "HTTP"
  vpc_id      = var.vpc_id
  target_type = "ip"

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
