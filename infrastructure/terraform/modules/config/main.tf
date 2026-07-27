data "aws_caller_identity" "current" {}

# ---------------------------------------------------------------------------
# S3 Bucket (AWS Config 記録データ保存用)
# ---------------------------------------------------------------------------
resource "aws_s3_bucket" "config" {
  bucket        = "${var.project}-${var.env}-config-bucket"
  force_destroy = true

  tags = {
    Name    = "${var.project}-${var.env}-config-bucket"
    Project = var.project
    Env     = var.env
  }
}

resource "aws_s3_bucket_public_access_block" "config" {
  bucket = aws_s3_bucket.config.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "config" {
  bucket = aws_s3_bucket.config.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# AWS Config公式ドキュメントのバケットポリシー要件（confused deputy対策のSourceAccount条件を含む）
resource "aws_s3_bucket_policy" "config" {
  bucket = aws_s3_bucket.config.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "AWSConfigBucketPermissionsCheck"
        Effect    = "Allow"
        Principal = { Service = "config.amazonaws.com" }
        Action    = "s3:GetBucketAcl"
        Resource  = aws_s3_bucket.config.arn
        Condition = {
          StringEquals = {
            "AWS:SourceAccount" = data.aws_caller_identity.current.account_id
          }
        }
      },
      {
        Sid       = "AWSConfigBucketExistenceCheck"
        Effect    = "Allow"
        Principal = { Service = "config.amazonaws.com" }
        Action    = "s3:ListBucket"
        Resource  = aws_s3_bucket.config.arn
        Condition = {
          StringEquals = {
            "AWS:SourceAccount" = data.aws_caller_identity.current.account_id
          }
        }
      },
      {
        Sid       = "AWSConfigBucketDelivery"
        Effect    = "Allow"
        Principal = { Service = "config.amazonaws.com" }
        Action    = "s3:PutObject"
        Resource  = "${aws_s3_bucket.config.arn}/AWSLogs/${data.aws_caller_identity.current.account_id}/Config/*"
        Condition = {
          StringEquals = {
            "s3:x-amz-acl"      = "bucket-owner-full-control"
            "AWS:SourceAccount" = data.aws_caller_identity.current.account_id
          }
        }
      }
    ]
  })
}

# ---------------------------------------------------------------------------
# IAM Role for AWS Config
# ---------------------------------------------------------------------------
resource "aws_iam_role" "config" {
  name = "${var.project}-${var.env}-config-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "config.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })

  tags = {
    Project = var.project
    Env     = var.env
  }
}

resource "aws_iam_role_policy_attachment" "config" {
  role       = aws_iam_role.config.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWS_ConfigRole"
}

# ---------------------------------------------------------------------------
# Configuration Recorder / Delivery Channel
# ---------------------------------------------------------------------------
resource "aws_config_configuration_recorder" "this" {
  name     = "${var.project}-${var.env}-config-recorder"
  role_arn = aws_iam_role.config.arn

  recording_group {
    all_supported = false # コスト抑制のため対象リソースタイプを明示的に限定する

    resource_types = [
      "AWS::EC2::SecurityGroup",
      "AWS::RDS::DBInstance",
      "AWS::S3::Bucket",
      "AWS::ElastiCache::CacheCluster",
      "AWS::IAM::Role",
      "AWS::EC2::Volume", # encrypted-volumesルールの評価対象。現状Fargateのみ運用のためEBSは実質存在せずコスト影響は無視できる
    ]
  }
}

resource "aws_config_delivery_channel" "this" {
  name           = "${var.project}-${var.env}-config-delivery-channel"
  s3_bucket_name = aws_s3_bucket.config.bucket

  snapshot_delivery_properties {
    delivery_frequency = "TwentyFour_Hours" # コスト抑制のため最低頻度
  }

  depends_on = [aws_config_configuration_recorder.this]
}

resource "aws_config_configuration_recorder_status" "this" {
  name       = aws_config_configuration_recorder.this.name
  is_enabled = true

  depends_on = [aws_config_delivery_channel.this]
}

# ---------------------------------------------------------------------------
# Config Managed Rules（コンフォーマンスパックは使わず個別ルールで最小構成）
# ---------------------------------------------------------------------------

# SecurityGroupでSSH(22番)が0.0.0.0/0に開放されていないかを検知
resource "aws_config_config_rule" "restricted_ssh" {
  name = "${var.project}-${var.env}-restricted-ssh"

  source {
    owner             = "AWS"
    source_identifier = "INCOMING_SSH_DISABLED"
  }

  tags = {
    Project = var.project
    Env     = var.env
  }

  depends_on = [aws_config_configuration_recorder_status.this]
}

# RDSインスタンスが誤ってパブリックアクセス可能になっていないかを検知
resource "aws_config_config_rule" "rds_public_access" {
  name = "${var.project}-${var.env}-rds-instance-public-access-check"

  source {
    owner             = "AWS"
    source_identifier = "RDS_INSTANCE_PUBLIC_ACCESS_CHECK"
  }

  tags = {
    Project = var.project
    Env     = var.env
  }

  depends_on = [aws_config_configuration_recorder_status.this]
}

# S3バケットが誤って読み取り公開になっていないかを検知
resource "aws_config_config_rule" "s3_public_read_prohibited" {
  name = "${var.project}-${var.env}-s3-bucket-public-read-prohibited"

  source {
    owner             = "AWS"
    source_identifier = "S3_BUCKET_PUBLIC_READ_PROHIBITED"
  }

  tags = {
    Project = var.project
    Env     = var.env
  }

  depends_on = [aws_config_configuration_recorder_status.this]
}

# S3バケットが誤って書き込み公開になっていないかを検知
resource "aws_config_config_rule" "s3_public_write_prohibited" {
  name = "${var.project}-${var.env}-s3-bucket-public-write-prohibited"

  source {
    owner             = "AWS"
    source_identifier = "S3_BUCKET_PUBLIC_WRITE_PROHIBITED"
  }

  tags = {
    Project = var.project
    Env     = var.env
  }

  depends_on = [aws_config_configuration_recorder_status.this]
}

# EBSボリュームが暗号化されているかを検知（現状Fargateのみ運用のため評価対象は実質存在しないが、将来EC2/EBSを使う際の検知網として設置）
resource "aws_config_config_rule" "encrypted_volumes" {
  name = "${var.project}-${var.env}-encrypted-volumes"

  source {
    owner             = "AWS"
    source_identifier = "ENCRYPTED_VOLUMES"
  }

  tags = {
    Project = var.project
    Env     = var.env
  }

  depends_on = [aws_config_configuration_recorder_status.this]
}

# IAMパスワードポリシーがアカウントのベースラインを満たしているかを検知（アカウント単位のグローバルルールのためリソースタイプの記録には依存しない）
resource "aws_config_config_rule" "iam_password_policy" {
  name = "${var.project}-${var.env}-iam-password-policy"

  source {
    owner             = "AWS"
    source_identifier = "IAM_PASSWORD_POLICY"
  }

  tags = {
    Project = var.project
    Env     = var.env
  }

  depends_on = [aws_config_configuration_recorder_status.this]
}

# ---------------------------------------------------------------------------
# 通知（Config Rules Compliance Change → EventBridge → SNS）
# ---------------------------------------------------------------------------
resource "aws_sns_topic" "config_notifications" {
  name = "${var.project}-${var.env}-config-notifications"

  tags = {
    Project = var.project
    Env     = var.env
  }
}

resource "aws_sns_topic_policy" "config_notifications" {
  arn = aws_sns_topic.config_notifications.arn

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Sid       = "AllowEventBridgePublish"
      Effect    = "Allow"
      Principal = { Service = "events.amazonaws.com" }
      Action    = "SNS:Publish"
      Resource  = aws_sns_topic.config_notifications.arn
      Condition = {
        StringEquals = {
          "AWS:SourceAccount" = data.aws_caller_identity.current.account_id
        }
      }
    }]
  })
}

resource "aws_sns_topic_subscription" "config_notifications_email" {
  for_each = toset(var.notification_emails)

  topic_arn = aws_sns_topic.config_notifications.arn
  protocol  = "email"
  endpoint  = each.value
}

resource "aws_cloudwatch_event_rule" "config_noncompliant" {
  name        = "${var.project}-${var.env}-config-noncompliant"
  description = "AWS Configルールの評価結果がNON_COMPLIANTになったことを検知する"

  event_pattern = jsonencode({
    source      = ["aws.config"]
    detail-type = ["Config Rules Compliance Change"]
    detail = {
      messageType = ["ComplianceChangeNotification"]
      newEvaluationResult = {
        complianceType = ["NON_COMPLIANT"]
      }
    }
  })

  tags = {
    Project = var.project
    Env     = var.env
  }
}

resource "aws_cloudwatch_event_target" "config_noncompliant_sns" {
  rule = aws_cloudwatch_event_rule.config_noncompliant.name
  arn  = aws_sns_topic.config_notifications.arn

  input_transformer {
    input_paths = {
      ruleName     = "$.detail.configRuleName"
      resourceType = "$.detail.resourceType"
      resourceId   = "$.detail.resourceId"
      compliance   = "$.detail.newEvaluationResult.complianceType"
      account      = "$.account"
      region       = "$.region"
    }

    input_template = "\"[AWS Config] <compliance>: ルール=<ruleName> リソースタイプ=<resourceType> リソースID=<resourceId> (アカウント: <account>, リージョン: <region>)\""
  }

  depends_on = [aws_sns_topic_policy.config_notifications]
}
