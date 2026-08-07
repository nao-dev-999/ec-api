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
