# ---------------------------------------------------------------------------
# 通知先 SNS Topic
# ---------------------------------------------------------------------------
resource "aws_sns_topic" "alarms" {
  name = "${var.project}-${var.env}-alarms"

  tags = {
    Project = var.project
    Env     = var.env
  }
}

resource "aws_sns_topic_subscription" "alarms_email" {
  for_each = toset(var.notification_emails)

  topic_arn = aws_sns_topic.alarms.arn
  protocol  = "email"
  endpoint  = each.value
}

# ---------------------------------------------------------------------------
# アプリログのERRORレベル検知
# JSON構造化ログ（logback-spring.xmlのLogstashEncoder）の level フィールドを検索する
# ---------------------------------------------------------------------------
resource "aws_cloudwatch_log_metric_filter" "app_error" {
  name           = "${var.project}-${var.env}-app-error-log"
  log_group_name = var.app_log_group_name
  pattern        = "{ $.level = \"ERROR\" }"

  metric_transformation {
    name          = "AppErrorLogCount"
    namespace     = "${var.project}-${var.env}/App"
    value         = "1"
    default_value = "0"
    unit          = "Count"
  }
}

resource "aws_cloudwatch_metric_alarm" "app_error" {
  alarm_name        = "${var.project}-${var.env}-app-error-log"
  alarm_description = "アプリログにERRORレベルのログが出力された"

  namespace   = aws_cloudwatch_log_metric_filter.app_error.metric_transformation[0].namespace
  metric_name = aws_cloudwatch_log_metric_filter.app_error.metric_transformation[0].name

  statistic           = "Sum"
  period              = 60
  evaluation_periods  = 1
  threshold           = 1
  comparison_operator = "GreaterThanOrEqualToThreshold"
  # ログが出ていない期間は「エラー無し」として扱う（メトリクスフィルタが1件も一致しない期間はデータポイント自体が発生しないため）
  treat_missing_data = "notBreaching"

  alarm_actions = [aws_sns_topic.alarms.arn]
  ok_actions    = [aws_sns_topic.alarms.arn]

  tags = {
    Project = var.project
    Env     = var.env
  }
}

# ---------------------------------------------------------------------------
# ECSサービスの起動中タスク数が0（サービス停止）を検知
# Container Insights（ecsモジュールでcluster設定済み）が出すRunningTaskCountを使用
# ---------------------------------------------------------------------------
resource "aws_cloudwatch_metric_alarm" "service_running_count_zero" {
  alarm_name        = "${var.project}-${var.env}-ecs-running-count-zero"
  alarm_description = "ECSサービスの起動中タスク数が0になった"

  namespace   = "ECS/ContainerInsights"
  metric_name = "RunningTaskCount"
  dimensions = {
    ClusterName = var.ecs_cluster_name
    ServiceName = var.ecs_service_name
  }

  statistic           = "Minimum"
  period              = 60
  evaluation_periods  = 2
  threshold           = 1
  comparison_operator = "LessThanThreshold"
  # データが取得できない状態自体が異常（サービス停止）の可能性があるため、欠測はアラーム状態として扱う
  treat_missing_data = "breaching"

  alarm_actions = [aws_sns_topic.alarms.arn]
  ok_actions    = [aws_sns_topic.alarms.arn]

  tags = {
    Project = var.project
    Env     = var.env
  }
}

# ---------------------------------------------------------------------------
# ECSサービスのCPU使用率が90%以上を検知
# ---------------------------------------------------------------------------
resource "aws_cloudwatch_metric_alarm" "cpu_high" {
  alarm_name        = "${var.project}-${var.env}-ecs-cpu-high"
  alarm_description = "ECSサービスのCPU使用率が90%以上"

  namespace   = "AWS/ECS"
  metric_name = "CPUUtilization"
  dimensions = {
    ClusterName = var.ecs_cluster_name
    ServiceName = var.ecs_service_name
  }

  statistic           = "Average"
  period              = 300
  evaluation_periods  = 3
  threshold           = 90
  comparison_operator = "GreaterThanOrEqualToThreshold"
  treat_missing_data  = "notBreaching"

  alarm_actions = [aws_sns_topic.alarms.arn]
  ok_actions    = [aws_sns_topic.alarms.arn]

  tags = {
    Project = var.project
    Env     = var.env
  }
}

# ---------------------------------------------------------------------------
# ECSサービスのメモリ使用率が90%以上を検知（OOM Killedの前兆）
# ---------------------------------------------------------------------------
resource "aws_cloudwatch_metric_alarm" "memory_high" {
  alarm_name        = "${var.project}-${var.env}-ecs-memory-high"
  alarm_description = "ECSサービスのメモリ使用率が90%以上"

  namespace   = "AWS/ECS"
  metric_name = "MemoryUtilization"
  dimensions = {
    ClusterName = var.ecs_cluster_name
    ServiceName = var.ecs_service_name
  }

  statistic           = "Average"
  period              = 300
  evaluation_periods  = 3
  threshold           = 90
  comparison_operator = "GreaterThanOrEqualToThreshold"
  treat_missing_data  = "notBreaching"

  alarm_actions = [aws_sns_topic.alarms.arn]
  ok_actions    = [aws_sns_topic.alarms.arn]

  tags = {
    Project = var.project
    Env     = var.env
  }
}

# ---------------------------------------------------------------------------
# ALB: ヘルスチェック失敗ターゲットの検知
# 「起動数0」より先に、デプロイ失敗やヘルスチェックNGを早期検知する
# ---------------------------------------------------------------------------
resource "aws_cloudwatch_metric_alarm" "alb_unhealthy_host" {
  alarm_name        = "${var.project}-${var.env}-alb-unhealthy-host"
  alarm_description = "ALBターゲットグループにヘルスチェック失敗のタスクが存在する"

  namespace   = "AWS/ApplicationELB"
  metric_name = "UnHealthyHostCount"
  dimensions = {
    LoadBalancer = var.alb_arn_suffix
    TargetGroup  = var.alb_target_group_arn_suffix
  }

  statistic           = "Maximum"
  period              = 60
  evaluation_periods  = 2
  threshold           = 0
  comparison_operator = "GreaterThanThreshold"
  treat_missing_data  = "notBreaching"

  alarm_actions = [aws_sns_topic.alarms.arn]
  ok_actions    = [aws_sns_topic.alarms.arn]

  tags = {
    Project = var.project
    Env     = var.env
  }
}

# ---------------------------------------------------------------------------
# ALB: アプリが返した5xxレスポンスの急増を検知
# GlobalExceptionHandlerのERRORログでは拾えない経路（フィルタ層等）の失敗も検知できる
# ---------------------------------------------------------------------------
resource "aws_cloudwatch_metric_alarm" "alb_target_5xx" {
  alarm_name        = "${var.project}-${var.env}-alb-target-5xx"
  alarm_description = "ALB配下のアプリが返す5xxレスポンスが急増している"

  namespace   = "AWS/ApplicationELB"
  metric_name = "HTTPCode_Target_5XX_Count"
  dimensions = {
    LoadBalancer = var.alb_arn_suffix
  }

  statistic           = "Sum"
  period              = 300
  evaluation_periods  = 1
  threshold           = 5
  comparison_operator = "GreaterThanOrEqualToThreshold"
  treat_missing_data  = "notBreaching"

  alarm_actions = [aws_sns_topic.alarms.arn]
  ok_actions    = [aws_sns_topic.alarms.arn]

  tags = {
    Project = var.project
    Env     = var.env
  }
}

# ---------------------------------------------------------------------------
# RDS: CPU使用率が90%以上を検知
# ---------------------------------------------------------------------------
resource "aws_cloudwatch_metric_alarm" "rds_cpu_high" {
  alarm_name        = "${var.project}-${var.env}-rds-cpu-high"
  alarm_description = "RDSインスタンスのCPU使用率が90%以上"

  namespace   = "AWS/RDS"
  metric_name = "CPUUtilization"
  dimensions = {
    DBInstanceIdentifier = var.rds_instance_id
  }

  statistic           = "Average"
  period              = 300
  evaluation_periods  = 3
  threshold           = 90
  comparison_operator = "GreaterThanOrEqualToThreshold"
  treat_missing_data  = "notBreaching"

  alarm_actions = [aws_sns_topic.alarms.arn]
  ok_actions    = [aws_sns_topic.alarms.arn]

  tags = {
    Project = var.project
    Env     = var.env
  }
}

# ---------------------------------------------------------------------------
# RDS: コネクション数の逼迫を検知（接続枯渇はアプリ全体に波及するため）
# ---------------------------------------------------------------------------
resource "aws_cloudwatch_metric_alarm" "rds_connections_high" {
  alarm_name        = "${var.project}-${var.env}-rds-connections-high"
  alarm_description = "RDSインスタンスのDatabaseConnectionsが閾値を超えた"

  namespace   = "AWS/RDS"
  metric_name = "DatabaseConnections"
  dimensions = {
    DBInstanceIdentifier = var.rds_instance_id
  }

  statistic           = "Average"
  period              = 300
  evaluation_periods  = 2
  threshold           = var.rds_database_connections_threshold
  comparison_operator = "GreaterThanOrEqualToThreshold"
  treat_missing_data  = "notBreaching"

  alarm_actions = [aws_sns_topic.alarms.arn]
  ok_actions    = [aws_sns_topic.alarms.arn]

  tags = {
    Project = var.project
    Env     = var.env
  }
}

# ---------------------------------------------------------------------------
# ElastiCache Redis: CPU使用率（セッションストア。落ちるとログイン継続が不可能になる）
# T系ノードはCPUUtilizationが管理プロセス分を差し引いた値になり実態より低く出ることがあるため、
# AWS推奨のEngineCPUUtilization（Redisエンジン自体の使用率）を使用する
# ---------------------------------------------------------------------------
resource "aws_cloudwatch_metric_alarm" "redis_cpu_high" {
  alarm_name        = "${var.project}-${var.env}-redis-cpu-high"
  alarm_description = "ElastiCache RedisのEngineCPUUtilizationが90%以上"

  namespace   = "AWS/ElastiCache"
  metric_name = "EngineCPUUtilization"
  dimensions = {
    CacheClusterId = var.redis_cluster_id
  }

  statistic           = "Average"
  period              = 300
  evaluation_periods  = 3
  threshold           = 90
  comparison_operator = "GreaterThanOrEqualToThreshold"
  treat_missing_data  = "notBreaching"

  alarm_actions = [aws_sns_topic.alarms.arn]
  ok_actions    = [aws_sns_topic.alarms.arn]

  tags = {
    Project = var.project
    Env     = var.env
  }
}

# ---------------------------------------------------------------------------
# ElastiCache Redis: Evictions発生を検知（メモリ逼迫によるセッションデータ強制削除）
# ---------------------------------------------------------------------------
resource "aws_cloudwatch_metric_alarm" "redis_evictions" {
  alarm_name        = "${var.project}-${var.env}-redis-evictions"
  alarm_description = "ElastiCache Redisでメモリ逼迫によるEvictionsが発生している"

  namespace   = "AWS/ElastiCache"
  metric_name = "Evictions"
  dimensions = {
    CacheClusterId = var.redis_cluster_id
  }

  statistic           = "Sum"
  period              = 300
  evaluation_periods  = 1
  threshold           = 1
  comparison_operator = "GreaterThanOrEqualToThreshold"
  treat_missing_data  = "notBreaching"

  alarm_actions = [aws_sns_topic.alarms.arn]
  ok_actions    = [aws_sns_topic.alarms.arn]

  tags = {
    Project = var.project
    Env     = var.env
  }
}

# ---------------------------------------------------------------------------
# ElastiCache Redis: 空きメモリの低下を検知（cache.t3.micro想定。総メモリ約0.5GiBの1割を閾値とする）
# ---------------------------------------------------------------------------
# ---------------------------------------------------------------------------
# CloudWatchダッシュボード（上記アラームの元メトリクスを1画面に集約）
# ---------------------------------------------------------------------------
locals {
  dashboard_widgets = [
    {
      type   = "metric"
      x      = 0
      y      = 0
      width  = 12
      height = 6
      properties = {
        title  = "ECS CPU / メモリ使用率"
        view   = "timeSeries"
        region = var.aws_region
        period = 300
        metrics = [
          ["AWS/ECS", "CPUUtilization", "ClusterName", var.ecs_cluster_name, "ServiceName", var.ecs_service_name, { label = "CPU" }],
          ["AWS/ECS", "MemoryUtilization", "ClusterName", var.ecs_cluster_name, "ServiceName", var.ecs_service_name, { label = "Memory" }],
        ]
      }
    },
    {
      type   = "metric"
      x      = 12
      y      = 0
      width  = 12
      height = 6
      properties = {
        title  = "ECS 起動中タスク数"
        view   = "timeSeries"
        region = var.aws_region
        period = 60
        metrics = [
          ["ECS/ContainerInsights", "RunningTaskCount", "ClusterName", var.ecs_cluster_name, "ServiceName", var.ecs_service_name],
        ]
      }
    },
    {
      type   = "metric"
      x      = 0
      y      = 6
      width  = 12
      height = 6
      properties = {
        title  = "ALB ヘルスチェック失敗 / 5xx"
        view   = "timeSeries"
        region = var.aws_region
        period = 300
        metrics = [
          ["AWS/ApplicationELB", "UnHealthyHostCount", "LoadBalancer", var.alb_arn_suffix, "TargetGroup", var.alb_target_group_arn_suffix, { label = "UnHealthyHostCount", yAxis = "left" }],
          ["AWS/ApplicationELB", "HTTPCode_Target_5XX_Count", "LoadBalancer", var.alb_arn_suffix, { label = "5xx", stat = "Sum", yAxis = "right" }],
        ]
      }
    },
    {
      type   = "metric"
      x      = 12
      y      = 6
      width  = 12
      height = 6
      properties = {
        title  = "RDS CPU / コネクション数"
        view   = "timeSeries"
        region = var.aws_region
        period = 300
        metrics = [
          ["AWS/RDS", "CPUUtilization", "DBInstanceIdentifier", var.rds_instance_id, { label = "CPU", yAxis = "left" }],
          ["AWS/RDS", "DatabaseConnections", "DBInstanceIdentifier", var.rds_instance_id, { label = "Connections", yAxis = "right" }],
        ]
      }
    },
    {
      type   = "metric"
      x      = 0
      y      = 12
      width  = 12
      height = 6
      properties = {
        title  = "ElastiCache Redis"
        view   = "timeSeries"
        region = var.aws_region
        period = 300
        metrics = [
          ["AWS/ElastiCache", "EngineCPUUtilization", "CacheClusterId", var.redis_cluster_id, { label = "EngineCPU" }],
          ["AWS/ElastiCache", "Evictions", "CacheClusterId", var.redis_cluster_id, { label = "Evictions", stat = "Sum", yAxis = "right" }],
          ["AWS/ElastiCache", "FreeableMemory", "CacheClusterId", var.redis_cluster_id, { label = "FreeableMemory", yAxis = "right" }],
        ]
      }
    },
    {
      type   = "metric"
      x      = 12
      y      = 12
      width  = 12
      height = 6
      properties = {
        title  = "アプリログ ERROR件数"
        view   = "timeSeries"
        region = var.aws_region
        period = 60
        stat   = "Sum"
        metrics = [
          [aws_cloudwatch_log_metric_filter.app_error.metric_transformation[0].namespace, aws_cloudwatch_log_metric_filter.app_error.metric_transformation[0].name],
        ]
      }
    },
  ]
}

resource "aws_cloudwatch_dashboard" "ops" {
  dashboard_name = "${var.project}-${var.env}-ops"
  dashboard_body = jsonencode({ widgets = local.dashboard_widgets })
}

resource "aws_cloudwatch_metric_alarm" "redis_freeable_memory_low" {
  alarm_name        = "${var.project}-${var.env}-redis-freeable-memory-low"
  alarm_description = "ElastiCache Redisの空きメモリが逼迫している"

  namespace   = "AWS/ElastiCache"
  metric_name = "FreeableMemory"
  dimensions = {
    CacheClusterId = var.redis_cluster_id
  }

  statistic           = "Average"
  period              = 300
  evaluation_periods  = 3
  threshold           = 50000000 # 約50MB
  comparison_operator = "LessThanThreshold"
  treat_missing_data  = "notBreaching"

  alarm_actions = [aws_sns_topic.alarms.arn]
  ok_actions    = [aws_sns_topic.alarms.arn]

  tags = {
    Project = var.project
    Env     = var.env
  }
}
