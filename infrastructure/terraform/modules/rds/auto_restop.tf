# 停止中のRDSが7日経過でAWSにより自動起動された場合に、自動で停止し直す仕組み。
# instance_state = "stopped" の間だけ作成する。
#
# 流れ: RDS-EVENT-0154(停止期間の上限超過による起動)をEventBridgeで検知
#       → SSM Automationで「available になるのを待ってから StopDBInstance」を実行(失敗時は待機して再試行)
#       → それでも失敗した場合はアラーム用SNSトピックへ通知
#
# RDS-EVENT-0154 はAWSによる自動起動の時だけ発行され、terraform apply(sleep_mode = false)による
# 通常の起動では発行されないため、スリープ解除の妨げにはならない。
# 起動処理中(starting)は停止できないため、AWS管理のAWS-StopRdsInstanceではなく待機付きの独自ドキュメントを使う。

locals {
  auto_restop_enabled = var.instance_state == "stopped"
}

data "aws_caller_identity" "current" {}

resource "aws_ssm_document" "restop" {
  count = local.auto_restop_enabled ? 1 : 0

  name            = "${var.project}-${var.env}-rds-restop"
  document_type   = "Automation"
  document_format = "JSON"

  content = jsonencode({
    schemaVersion = "0.3"
    description   = "自動起動されたRDSインスタンスを、起動完了を待ってから停止する"
    assumeRole    = "{{ AutomationAssumeRole }}"
    parameters = {
      InstanceId           = { type = "String" }
      AutomationAssumeRole = { type = "String" }
    }
    mainSteps = [
      {
        name           = "WaitForAvailable"
        action         = "aws:waitForAwsResourceProperty"
        timeoutSeconds = 3600
        inputs = {
          Service              = "rds"
          Api                  = "DescribeDBInstances"
          DBInstanceIdentifier = "{{ InstanceId }}"
          PropertySelector     = "$.DBInstances[0].DBInstanceStatus"
          DesiredValues        = ["available"]
        }
      },
      # 自動起動直後は再起動・Enhanced Monitoring/Performance Insightsの再設定・バックアップが続き、
      # available とそれ以外の状態を数分間行き来する。一瞬の available で停止するとInvalidDBInstanceStateで
      # 失敗するため、落ち着くまで待ってから再度 available を待つ。
      {
        name   = "WaitForPostStartTasks"
        action = "aws:sleep"
        inputs = { Duration = "PT15M" }
      },
      {
        name           = "WaitForAvailableAgain"
        action         = "aws:waitForAwsResourceProperty"
        timeoutSeconds = 3600
        inputs = {
          Service              = "rds"
          Api                  = "DescribeDBInstances"
          DBInstanceIdentifier = "{{ InstanceId }}"
          PropertySelector     = "$.DBInstances[0].DBInstanceStatus"
          DesiredValues        = ["available"]
        }
      },
      # maxAttemptsのリトライには待機が入らないため、状態の揺れが続いていると3回とも失敗しうる。
      # 失敗時は待機してから再度 available を待ち、もう一度だけ停止を試みる。
      #   isEnd      : 成功したらリトライ用ステップへ進まずに終了する
      #   isCritical : ここで失敗してもリトライ側で停止できれば、Automation全体をSuccessとして扱う
      {
        name        = "StopInstance"
        action      = "aws:executeAwsApi"
        maxAttempts = 3
        onFailure   = "step:WaitBeforeRetry"
        isCritical  = false
        isEnd       = true
        inputs = {
          Service              = "rds"
          Api                  = "StopDBInstance"
          DBInstanceIdentifier = "{{ InstanceId }}"
        }
      },
      {
        name   = "WaitBeforeRetry"
        action = "aws:sleep"
        inputs = { Duration = "PT5M" }
      },
      {
        name           = "WaitForAvailableRetry"
        action         = "aws:waitForAwsResourceProperty"
        timeoutSeconds = 3600
        inputs = {
          Service              = "rds"
          Api                  = "DescribeDBInstances"
          DBInstanceIdentifier = "{{ InstanceId }}"
          PropertySelector     = "$.DBInstances[0].DBInstanceStatus"
          DesiredValues        = ["available"]
        }
      },
      # 最後の停止ステップにはonFailureを付けない(無限ループ防止)。ここで失敗した場合は
      # Automationが Failed となり、下の restop_failed ルールで通知される。
      {
        name        = "StopInstanceRetry"
        action      = "aws:executeAwsApi"
        maxAttempts = 3
        inputs = {
          Service              = "rds"
          Api                  = "StopDBInstance"
          DBInstanceIdentifier = "{{ InstanceId }}"
        }
      }
    ]
  })
}

# SSM Automationが実行時に引き受けるロール(RDSの参照・停止のみ)
resource "aws_iam_role" "restop_automation" {
  count = local.auto_restop_enabled ? 1 : 0

  name = "${var.project}-${var.env}-rds-restop-automation"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect    = "Allow"
        Principal = { Service = "ssm.amazonaws.com" }
        Action    = "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_role_policy" "restop_automation" {
  count = local.auto_restop_enabled ? 1 : 0

  name = "rds-stop"
  role = aws_iam_role.restop_automation[0].id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["rds:DescribeDBInstances", "rds:StopDBInstance"]
        Resource = aws_db_instance.this.arn
      }
    ]
  })
}

# EventBridgeがSSM Automationを起動するためのロール
resource "aws_iam_role" "restop_events" {
  count = local.auto_restop_enabled ? 1 : 0

  name = "${var.project}-${var.env}-rds-restop-events"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect    = "Allow"
        Principal = { Service = "events.amazonaws.com" }
        Action    = "sts:AssumeRole"
      }
    ]
  })
}

resource "aws_iam_role_policy" "restop_events" {
  count = local.auto_restop_enabled ? 1 : 0

  name = "start-rds-restop-automation"
  role = aws_iam_role.restop_events[0].id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = "ssm:StartAutomationExecution"
        # 認可はautomation-definitionだけでなくdocument側のARNでも評価されるため両方を許可する。
        # (document側が無いとAccessDeniedになり、Automationの実行自体が作られない)
        # 注意: Automationタイプのaws_ssm_document.arnは automation-definition/<name> を返すため、
        # document/<name> は明示的に組み立てる。
        Resource = [
          "arn:aws:ssm:${var.aws_region}:${data.aws_caller_identity.current.account_id}:automation-definition/${aws_ssm_document.restop[0].name}:*",
          "arn:aws:ssm:${var.aws_region}:${data.aws_caller_identity.current.account_id}:document/${aws_ssm_document.restop[0].name}",
        ]
      },
      {
        Effect   = "Allow"
        Action   = "iam:PassRole"
        Resource = aws_iam_role.restop_automation[0].arn
      }
    ]
  })
}

resource "aws_cloudwatch_event_rule" "restop" {
  count = local.auto_restop_enabled ? 1 : 0

  name        = "${var.project}-${var.env}-rds-auto-started"
  description = "停止中のRDSが7日経過で自動起動されたことを検知する(RDS-EVENT-0154)"

  event_pattern = jsonencode({
    source        = ["aws.rds"]
    "detail-type" = ["RDS DB Instance Event"]
    detail = {
      SourceArn = [aws_db_instance.this.arn]
      EventID   = ["RDS-EVENT-0154"]
    }
  })
}

resource "aws_cloudwatch_event_target" "restop" {
  count = local.auto_restop_enabled ? 1 : 0

  rule     = aws_cloudwatch_event_rule.restop[0].name
  arn      = "arn:aws:ssm:${var.aws_region}:${data.aws_caller_identity.current.account_id}:automation-definition/${aws_ssm_document.restop[0].name}:$DEFAULT"
  role_arn = aws_iam_role.restop_events[0].arn

  input = jsonencode({
    InstanceId           = [aws_db_instance.this.identifier]
    AutomationAssumeRole = [aws_iam_role.restop_automation[0].arn]
  })
}

# ---------------------------------------------------------------------------
# 再停止の失敗検知
# Automationが Failed/TimedOut/Cancelled で終わった場合にアラーム用SNSトピックへ通知する。
# (RDSが起動したまま気付けない状態を防ぐ)
# ---------------------------------------------------------------------------
resource "aws_cloudwatch_event_rule" "restop_failed" {
  count = local.auto_restop_enabled ? 1 : 0

  name        = "${var.project}-${var.env}-rds-restop-failed"
  description = "RDS自動再停止のSSM Automationが失敗したことを検知する"

  event_pattern = jsonencode({
    source        = ["aws.ssm"]
    "detail-type" = ["EC2 Automation Execution Status-change Notification"]
    detail = {
      Definition = [aws_ssm_document.restop[0].name]
      Status     = ["Failed", "TimedOut", "Cancelled"]
    }
  })
}

resource "aws_cloudwatch_event_target" "restop_failed" {
  count = local.auto_restop_enabled ? 1 : 0

  rule = aws_cloudwatch_event_rule.restop_failed[0].name
  arn  = var.alarm_sns_topic_arn

  input_transformer {
    input_paths = {
      status      = "$.detail.Status"
      executionId = "$.detail.ExecutionId"
    }
    input_template = "\"[${var.project}-${var.env}] RDS自動再停止のSSM Automationが <status> で終了しました。RDS (${aws_db_instance.this.identifier}) が起動したままの可能性があります。ExecutionId: <executionId>\""
  }
}
