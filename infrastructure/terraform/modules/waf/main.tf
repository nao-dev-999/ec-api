# AWS WAFv2 WebACL: ALBの手前でIPアドレス単位のレートベース制限を行う。
# ここでの制限は全体を守るための粗いしきい値。
# エンドポイント単位・ユーザー単位の細かい制限はアプリ側のRateLimitingFilter（Bucket4j+Redis）が担当する。
resource "aws_wafv2_web_acl" "this" {
  name        = "${var.project}-${var.env}-waf"
  description = "Rate limiting for ${var.project}-${var.env} ALB"
  scope       = "REGIONAL"

  default_action {
    allow {}
  }

  # ログイン・サインアップ系: ブルートフォース/スパムアカウント作成対策として、より低いしきい値
  rule {
    name     = "auth-rate-limit"
    priority = 1

    action {
      block {}
    }

    statement {
      rate_based_statement {
        limit              = var.auth_rate_limit
        aggregate_key_type = "IP"

        scope_down_statement {
          or_statement {
            statement {
              byte_match_statement {
                search_string         = "/auth/login"
                positional_constraint = "CONTAINS"
                field_to_match {
                  uri_path {}
                }
                text_transformation {
                  priority = 0
                  type     = "NONE"
                }
              }
            }
            statement {
              byte_match_statement {
                search_string         = "/auth/signup"
                positional_constraint = "CONTAINS"
                field_to_match {
                  uri_path {}
                }
                text_transformation {
                  priority = 0
                  type     = "NONE"
                }
              }
            }
          }
        }
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "${var.project}-${var.env}-auth-rate-limit"
      sampled_requests_enabled   = true
    }
  }

  # 全体: 一般的な過負荷・ボットアクセス対策としての粗い足切り
  rule {
    name     = "general-rate-limit"
    priority = 2

    action {
      block {}
    }

    statement {
      rate_based_statement {
        limit              = var.general_rate_limit
        aggregate_key_type = "IP"
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "${var.project}-${var.env}-general-rate-limit"
      sampled_requests_enabled   = true
    }
  }

  visibility_config {
    cloudwatch_metrics_enabled = true
    metric_name                = "${var.project}-${var.env}-waf"
    sampled_requests_enabled   = true
  }

  tags = {
    Name = "${var.project}-${var.env}-waf"
  }
}

resource "aws_wafv2_web_acl_association" "alb" {
  resource_arn = var.alb_arn
  web_acl_arn  = aws_wafv2_web_acl.this.arn
}

# --- WAFログ配信用S3バケット ---
# CloudWatchメトリクス・サンプルリクエストだけでは過去の全リクエストを検索・分析できないため、
# フルログをS3に直接配信する（Kinesis Firehoseは使わない）。
# バケット名は AWS WAF の制約により "aws-waf-logs-" プレフィックスが必須。
resource "aws_s3_bucket" "waf_logs" {
  bucket = "aws-waf-logs-${var.project}-${var.env}"

  tags = {
    Name = "aws-waf-logs-${var.project}-${var.env}"
  }
}

# パブリックアクセスは一切許可しない（ログには送信元IP等の情報が含まれるため）
resource "aws_s3_bucket_public_access_block" "waf_logs" {
  bucket = aws_s3_bucket.waf_logs.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# コスト削減のため、一定期間経過後にオブジェクトを自動削除する
resource "aws_s3_bucket_lifecycle_configuration" "waf_logs" {
  bucket = aws_s3_bucket.waf_logs.id

  rule {
    id     = "expire-waf-logs"
    status = "Enabled"

    filter {}

    expiration {
      days = var.waf_log_retention_days
    }
  }
}

# WebACLのログをS3バケットへ直接配信する設定。
# S3を宛先にした場合、PutLoggingConfiguration実行時にAWS WAF側が
# バケットポリシー（書き込み許可）を自動作成するため、こちらでの追加設定は不要。
resource "aws_wafv2_web_acl_logging_configuration" "this" {
  resource_arn            = aws_wafv2_web_acl.this.arn
  log_destination_configs = [aws_s3_bucket.waf_logs.arn]

  # JWT認証のAuthorizationヘッダー、およびRedisセッションのCookieヘッダーは
  # 機密情報を含むため、ログから除外する。
  redacted_fields {
    single_header {
      name = "authorization"
    }
  }

  redacted_fields {
    single_header {
      name = "cookie"
    }
  }
}
