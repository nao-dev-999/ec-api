data "aws_caller_identity" "current" {}
data "aws_region" "current" {}

# ---------------------------------------------------------------------------
# CloudTrail: APIアクティビティの監査証跡
# AWS Configは「設定がどう変わったか」を追跡するが、CloudTrailは「誰が何を呼んだか」を記録する。
# 侵入・誤操作時の調査に必須のため、Config/GuardDutyとセットで用意する
# ---------------------------------------------------------------------------
resource "aws_s3_bucket" "cloudtrail" {
  bucket        = "${var.project}-${var.env}-cloudtrail"
  force_destroy = true

  tags = {
    Name    = "${var.project}-${var.env}-cloudtrail"
    Project = var.project
    Env     = var.env
  }
}

resource "aws_s3_bucket_public_access_block" "cloudtrail" {
  bucket = aws_s3_bucket.cloudtrail.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "cloudtrail" {
  bucket = aws_s3_bucket.cloudtrail.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# 監査ログの改ざん・誤削除を防ぐためバージョニングを有効化する
resource "aws_s3_bucket_versioning" "cloudtrail" {
  bucket = aws_s3_bucket.cloudtrail.id
  versioning_configuration {
    status = "Enabled"
  }
}

# コスト削減のため、一定期間経過後に自動削除する
resource "aws_s3_bucket_lifecycle_configuration" "cloudtrail" {
  bucket = aws_s3_bucket.cloudtrail.id

  rule {
    id     = "expire-cloudtrail-logs"
    status = "Enabled"

    filter {}

    expiration {
      days = var.cloudtrail_log_retention_days
    }

    noncurrent_version_expiration {
      noncurrent_days = var.cloudtrail_log_retention_days
    }

    abort_incomplete_multipart_upload {
      days_after_initiation = 7
    }
  }
}

# AWS公式ドキュメントのバケットポリシー要件（confused deputy対策のSourceArn条件を含む）
resource "aws_s3_bucket_policy" "cloudtrail" {
  bucket = aws_s3_bucket.cloudtrail.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "AWSCloudTrailAclCheck"
        Effect    = "Allow"
        Principal = { Service = "cloudtrail.amazonaws.com" }
        Action    = "s3:GetBucketAcl"
        Resource  = aws_s3_bucket.cloudtrail.arn
        Condition = {
          StringEquals = {
            "aws:SourceArn" = "arn:aws:cloudtrail:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:trail/${var.project}-${var.env}-trail"
          }
        }
      },
      {
        Sid       = "AWSCloudTrailWrite"
        Effect    = "Allow"
        Principal = { Service = "cloudtrail.amazonaws.com" }
        Action    = "s3:PutObject"
        Resource  = "${aws_s3_bucket.cloudtrail.arn}/AWSLogs/${data.aws_caller_identity.current.account_id}/*"
        Condition = {
          StringEquals = {
            "s3:x-amz-acl"  = "bucket-owner-full-control"
            "aws:SourceArn" = "arn:aws:cloudtrail:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:trail/${var.project}-${var.env}-trail"
          }
        }
      }
    ]
  })
}

resource "aws_cloudtrail" "this" {
  name           = "${var.project}-${var.env}-trail"
  s3_bucket_name = aws_s3_bucket.cloudtrail.id

  is_multi_region_trail        = true
  include_global_service_events = true
  enable_log_file_validation    = true

  tags = {
    Project = var.project
    Env     = var.env
  }

  depends_on = [aws_s3_bucket_policy.cloudtrail]
}

# ---------------------------------------------------------------------------
# GuardDuty: 不審なAPI呼び出し・C2通信等の脅威検知
# ---------------------------------------------------------------------------
resource "aws_guardduty_detector" "this" {
  enable = true

  tags = {
    Project = var.project
    Env     = var.env
  }
}

# 高深刻度（HIGH/CRITICAL、severity>=7）の検出のみ通知する。LOW/MEDIUMは調査コストに見合わないノイズになりやすいため対象外
resource "aws_cloudwatch_event_rule" "guardduty_high_severity" {
  name        = "${var.project}-${var.env}-guardduty-high-severity"
  description = "GuardDutyの高深刻度（severity>=7）検出を通知する"

  event_pattern = jsonencode({
    source      = ["aws.guardduty"]
    detail-type = ["GuardDuty Finding"]
    detail = {
      severity = [{ numeric = [">=", 7] }]
    }
  })

  tags = {
    Project = var.project
    Env     = var.env
  }
}

resource "aws_cloudwatch_event_target" "guardduty_high_severity_sns" {
  rule = aws_cloudwatch_event_rule.guardduty_high_severity.name
  arn  = var.sns_topic_arn

  input_transformer {
    input_paths = {
      severity = "$.detail.severity"
      type     = "$.detail.type"
      title    = "$.detail.title"
      account  = "$.account"
      region   = "$.region"
    }

    input_template = "\"[GuardDuty] severity=<severity> type=<type> - <title> (アカウント: <account>, リージョン: <region>)\""
  }
}
