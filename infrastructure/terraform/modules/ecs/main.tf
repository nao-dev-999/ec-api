data "aws_caller_identity" "current" {}

# ---------------------------------------------------------------------------
# ECS Cluster
# ---------------------------------------------------------------------------
resource "aws_ecs_cluster" "this" {
  name = "${var.project}-${var.env}-cluster"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  tags = {
    Name    = "${var.project}-${var.env}-cluster"
    Project = var.project
    Env     = var.env
  }
}

resource "aws_ecs_cluster_capacity_providers" "this" {
  cluster_name       = aws_ecs_cluster.this.name
  capacity_providers = ["FARGATE"]

  default_capacity_provider_strategy {
    capacity_provider = "FARGATE"
    weight            = 1
  }
}

# ---------------------------------------------------------------------------
# Security Group for ECS Tasks
# ---------------------------------------------------------------------------
resource "aws_security_group" "ecs" {
  name        = "${var.project}-${var.env}-ecs-sg"
  description = "Security group for ECS Fargate tasks"
  vpc_id      = var.vpc_id

  ingress {
    description     = "Allow from ALB (app)"
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [var.alb_sg_id]
  }

  ingress {
    description     = "Allow from ALB (frontend)"
    from_port       = 3000
    to_port         = 3000
    protocol        = "tcp"
    security_groups = [var.frontend_alb_sg_id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name    = "${var.project}-${var.env}-ecs-sg"
    Project = var.project
    Env     = var.env
  }
}

# ---------------------------------------------------------------------------
# Security Group for ElastiCache Redis
# ---------------------------------------------------------------------------
resource "aws_security_group" "redis" {
  name        = "${var.project}-${var.env}-redis-sg"
  description = "Security group for ElastiCache Redis"
  vpc_id      = var.vpc_id

  ingress {
    description     = "Allow from ECS tasks"
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name    = "${var.project}-${var.env}-redis-sg"
    Project = var.project
    Env     = var.env
  }
}

# ---------------------------------------------------------------------------
# ElastiCache Redis (Spring Session用)
# ---------------------------------------------------------------------------
resource "aws_elasticache_subnet_group" "this" {
  name       = "${var.project}-${var.env}-redis-subnet"
  subnet_ids = var.private_subnet_ids

  tags = {
    Name    = "${var.project}-${var.env}-redis-subnet"
    Project = var.project
    Env     = var.env
  }
}


# ElastiCache Redis(AUTH)用トークン。DBパスワードと異なりRDSのmanage_master_user_password
# のようなAWS管理の仕組みがElastiCacheには無いため、Secrets Managerで自前管理する。
# enable_redis = false でレプリケーショングループを削除している間も、再作成時に同じ値を
# 使い続けられるようトークン自体(SG・サブネットグループと同様に無料のリソース)は残す。
resource "random_password" "redis_auth_token" {
  length = 32
  # ElastiCacheのauth_tokenは "/", "\"", "@", 空白を含められないため、
  # 記号を使わず英数字のみにして制約を確実に満たす。
  special = false
}

resource "aws_secretsmanager_secret" "redis_auth_token" {
  name = "${var.project}-${var.env}-redis-auth-token"

  tags = {
    Name    = "${var.project}-${var.env}-redis-auth-token"
    Project = var.project
    Env     = var.env
  }
}

resource "aws_secretsmanager_secret_version" "redis_auth_token" {
  secret_id     = aws_secretsmanager_secret.redis_auth_token.id
  secret_string = random_password.redis_auth_token.result
}

locals {
  redis_replication_group_id = "${var.project}-${var.env}-redis"
  # enable_redis = false の間はタスク定義に入れる値が無いため、名前解決できないダミーのホスト名を入れておく
  redis_host = var.enable_redis ? aws_elasticache_replication_group.this[0].primary_endpoint_address : "redis-disabled.invalid"
}

# 転送時・保管時暗号化とAUTHを有効化するため、aws_elasticache_cluster(暗号化非対応)ではなく
# aws_elasticache_replication_group を使う。
# enable_redis = false の間はレプリケーショングループを削除する(SG・サブネットグループ・AUTHトークンは残す)。
# Redisにはセッション等の一時データしか置かない前提のため、データは引き継がない。
resource "aws_elasticache_replication_group" "this" {
  count = var.enable_redis ? 1 : 0

  replication_group_id       = local.redis_replication_group_id
  description                = "Redis for ${var.project}-${var.env}(Spring Session / rate limiting)"
  engine                     = "redis"
  engine_version             = "7.1"
  node_type                  = var.redis_node_type
  num_cache_clusters         = var.redis_num_cache_clusters
  automatic_failover_enabled = var.redis_num_cache_clusters > 1
  multi_az_enabled           = var.redis_num_cache_clusters > 1
  notification_topic_arn     = var.redis_notification_topic_arn
  parameter_group_name       = "default.redis7"
  port                       = 6379
  subnet_group_name          = aws_elasticache_subnet_group.this.name
  security_group_ids         = [aws_security_group.redis.id]

  at_rest_encryption_enabled = true
  transit_encryption_enabled = true
  auth_token                 = random_password.redis_auth_token.result

  tags = {
    Name    = "${var.project}-${var.env}-redis"
    Project = var.project
    Env     = var.env
  }
}

moved {
  from = aws_elasticache_replication_group.this
  to   = aws_elasticache_replication_group.this[0]
}

# ---------------------------------------------------------------------------
# CloudWatch Log Group
# ---------------------------------------------------------------------------
resource "aws_cloudwatch_log_group" "app" {
  name              = "/ecs/${var.project}-${var.env}/app"
  retention_in_days = 30

  tags = {
    Project = var.project
    Env     = var.env
  }
}

resource "aws_cloudwatch_log_group" "flyway" {
  name              = "/ecs/${var.project}-${var.env}/flyway"
  retention_in_days = 30

  tags = {
    Project = var.project
    Env     = var.env
  }
}

# ---------------------------------------------------------------------------
# IAM Role for ECS Task Execution
# ---------------------------------------------------------------------------
resource "aws_iam_role" "task_execution" {
  name = "${var.project}-${var.env}-ecs-task-execution-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })

  tags = {
    Project = var.project
    Env     = var.env
  }
}

resource "aws_iam_role_policy_attachment" "task_execution" {
  role       = aws_iam_role.task_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

# Secrets Manager / SSM Parameter Store へのアクセス許可
resource "aws_iam_role_policy" "task_execution_secrets" {
  name = "secrets-access"
  role = aws_iam_role.task_execution.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "secretsmanager:GetSecretValue",
          "ssm:GetParameter",
          "ssm:GetParameters",
          "kms:Decrypt"
        ]
        Resource = "*"
      }
    ]
  })
}

# ---------------------------------------------------------------------------
# IAM Role for ECS Task (アプリ実行時の権限)
# ---------------------------------------------------------------------------
resource "aws_iam_role" "task" {
  name = "${var.project}-${var.env}-ecs-task-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })

  tags = {
    Project = var.project
    Env     = var.env
  }
}

# ECS Exec（execute-command）でコンテナにシェル接続するために必要
resource "aws_iam_role_policy" "task_exec_ssm" {
  name = "ecs-exec-ssmmessages"
  role = aws_iam_role.task.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "ssmmessages:CreateControlChannel",
          "ssmmessages:CreateDataChannel",
          "ssmmessages:OpenControlChannel",
          "ssmmessages:OpenDataChannel"
        ]
        Resource = "*"
      }
    ]
  })
}

# ---------------------------------------------------------------------------
# 現在デプロイ済みのイメージの解決
# ---------------------------------------------------------------------------
# イメージのタグ(コミットSHA)はCodePipelineが決めるため、Terraformはタグを固定せず
# 「今動いているイメージ」をAWSから読み取ってタスク定義に入れる。これにより、
# Terraformでタスク定義の設定(CPU/メモリ/環境変数等)を変えてapplyしたとき、
# イメージはそのままで設定だけが反映される。
# *_image_tag を明示した場合(初回構築時など)はその値を優先し、読み取りは行わない。

# app: ECSサービスが現在使っているタスク定義のイメージ
data "aws_ecs_service" "app_current" {
  count        = var.app_image_tag == null ? 1 : 0
  service_name = "${var.project}-${var.env}-app-service"
  cluster_arn  = "arn:aws:ecs:${var.aws_region}:${data.aws_caller_identity.current.account_id}:cluster/${var.project}-${var.env}-cluster"
}

data "aws_ecs_container_definition" "app_current" {
  count           = var.app_image_tag == null ? 1 : 0
  task_definition = data.aws_ecs_service.app_current[0].task_definition
  container_name  = "app"
}

# batch: サービスが無いため、ファミリーの最新ACTIVEリビジョン(Step Functionsが実行するもの)のイメージ
data "aws_ecs_container_definition" "batch_current" {
  count           = var.batch_image_tag == null ? 1 : 0
  task_definition = "${var.project}-${var.env}-batch"
  container_name  = "batch"
}

locals {
  app_image   = var.app_image_tag != null ? "${var.app_image_url}:${var.app_image_tag}" : data.aws_ecs_container_definition.app_current[0].image
  batch_image = var.batch_image_tag != null ? "${var.batch_image_url}:${var.batch_image_tag}" : data.aws_ecs_container_definition.batch_current[0].image
}

# ---------------------------------------------------------------------------
# ECS Task Definition - App
# ---------------------------------------------------------------------------
resource "aws_ecs_task_definition" "app" {
  family                   = "${var.project}-${var.env}-app"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.task_cpu
  memory                   = var.task_memory
  execution_role_arn       = aws_iam_role.task_execution.arn
  task_role_arn            = aws_iam_role.task.arn

  container_definitions = jsonencode([
    {
      name      = "app"
      image     = local.app_image
      essential = true

      # SIGTERM送信からSIGKILLまでの猶予秒数。Fargateデフォルトと同値(30秒)だが、
      # ALBのderegistration_delay・アプリのtimeout-per-shutdown-phase(25秒)との
      # 整合が重要なため明示する。
      stopTimeout = 30

      portMappings = [{
        containerPort = 8080
        protocol      = "tcp"
      }]

      environment = [
        { name = "SPRING_PROFILES_ACTIVE", value = var.env },
        { name = "SPRING_DATASOURCE_URL", value = "jdbc:postgresql://${var.db_host}:5432/${var.db_name}" },
        { name = "SPRING_DATA_REDIS_HOST", value = local.redis_host },
        { name = "SPRING_DATA_REDIS_PORT", value = "6379" },
        { name = "SPRING_DATA_REDIS_SSL_ENABLED", value = "true" },
        # Flywayはアプリ起動時には無効化（別タスクで実行）
        { name = "SPRING_FLYWAY_ENABLED", value = "false" },
        # Swagger UI/OpenAPI定義の公開有無(OpenApiConfig/application.yml参照)。
        { name = "APP_SWAGGER_ENABLED", value = tostring(var.swagger_enabled) },
        # frontendからのCORS許可オリジン(SecurityConfig.allowedOrigins参照。デフォルトはlocalhost:3000)
        { name = "APP_CORS_ALLOWED_ORIGINS", value = var.frontend_public_url },
      ]

      secrets = [
        {
          name      = "SPRING_DATASOURCE_USERNAME"
          valueFrom = "${var.db_password_secret_arn}:username::"
        },
        {
          name      = "SPRING_DATASOURCE_PASSWORD"
          valueFrom = "${var.db_password_secret_arn}:password::"
        },
        {
          name      = "SPRING_DATA_REDIS_PASSWORD"
          valueFrom = aws_secretsmanager_secret.redis_auth_token.arn
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.app.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "app"
        }
      }

      healthCheck = {
        command     = ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 60
      }
    }
  ])

  lifecycle {
    # パイプラインはlatestタグをpushしないため、latestを指すとタスクが起動できない
    precondition {
      condition     = !endswith(local.app_image, ":latest")
      error_message = "appイメージが :latest を指しています。CodePipelineでデプロイ済みのタグ(コミットSHA)を app_image_tag に指定してください。"
    }
  }

  tags = {
    Project = var.project
    Env     = var.env
  }
}

# ---------------------------------------------------------------------------
# ECS Task Definition - Flyway (マイグレーション専用)
# ---------------------------------------------------------------------------
resource "aws_ecs_task_definition" "flyway" {
  family                   = "${var.project}-${var.env}-flyway"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "512"
  memory                   = "1024"
  execution_role_arn       = aws_iam_role.task_execution.arn
  task_role_arn            = aws_iam_role.task.arn

  container_definitions = jsonencode([
    {
      name = "flyway"
      # Flyway公式CLIイメージ（マイグレーションSQLを同梱してビルド）を使用
      image     = "${var.flyway_image_url}:${var.flyway_image_tag}"
      essential = true

      # ECS.5(FSBP): ルートファイルシステムを読み取り専用に制限。
      # Flywayはレポート生成等で一時ファイルを書く場合があるため/tmpのみ書き込み可能にする
      readonlyRootFilesystem = true
      mountPoints = [
        { sourceVolume = "tmp", containerPath = "/tmp", readOnly = false }
      ]

      command = ["migrate"]

      environment = [
        { name = "FLYWAY_URL", value = "jdbc:postgresql://${var.db_host}:5432/${var.db_name}" },
        { name = "FLYWAY_LOCATIONS", value = "filesystem:/flyway/sql" },
        { name = "FLYWAY_BASELINE_ON_MIGRATE", value = "true" },
      ]

      secrets = [
        {
          name      = "FLYWAY_USER"
          valueFrom = "${var.db_password_secret_arn}:username::"
        },
        {
          name      = "FLYWAY_PASSWORD"
          valueFrom = "${var.db_password_secret_arn}:password::"
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.flyway.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "flyway"
        }
      }
    }
  ])

  volume {
    name = "tmp"
  }

  tags = {
    Project = var.project
    Env     = var.env
  }
}

# ---------------------------------------------------------------------------
# ECS Task Definition - Batch（日次集計バッチ。ALB無し、EventBridge Schedulerから都度RunTask）
# ---------------------------------------------------------------------------
resource "aws_cloudwatch_log_group" "batch" {
  name              = "/ecs/${var.project}-${var.env}/batch"
  retention_in_days = 30

  tags = {
    Project = var.project
    Env     = var.env
  }
}

resource "aws_ecs_task_definition" "batch" {
  family                   = "${var.project}-${var.env}-batch"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.batch_cpu
  memory                   = var.batch_memory
  execution_role_arn       = aws_iam_role.task_execution.arn
  task_role_arn            = aws_iam_role.task.arn

  container_definitions = jsonencode([
    {
      name      = "batch"
      image     = local.batch_image
      essential = true

      # ECS.5(FSBP): ルートファイルシステムを読み取り専用に制限。
      # JVMのjava.io.tmpdir、および帳票出力ジョブの一時ファイル用に/tmpのみ書き込み可能にする
      readonlyRootFilesystem = true
      mountPoints = [
        { sourceVolume = "tmp", containerPath = "/tmp", readOnly = false }
      ]

      environment = [
        { name = "SPRING_PROFILES_ACTIVE", value = var.env },
        { name = "SPRING_DATASOURCE_URL", value = "jdbc:postgresql://${var.db_host}:5432/${var.db_name}" },
      ]

      secrets = [
        {
          name      = "SPRING_DATASOURCE_USERNAME"
          valueFrom = "${var.db_password_secret_arn}:username::"
        },
        {
          name      = "SPRING_DATASOURCE_PASSWORD"
          valueFrom = "${var.db_password_secret_arn}:password::"
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.batch.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "batch"
        }
      }
    }
  ])

  volume {
    name = "tmp"
  }

  lifecycle {
    # パイプラインはlatestタグをpushしないため、latestを指すとタスクが起動できない
    precondition {
      condition     = !endswith(local.batch_image, ":latest")
      error_message = "batchイメージが :latest を指しています。batchパイプラインを実行してSHAタグのリビジョンを登録するか、batch_image_tag にタグを指定してください。"
    }
  }

  tags = {
    Project = var.project
    Env     = var.env
  }
}

resource "aws_iam_role" "sfn_batch_orchestrator" {
  name = "${var.project}-${var.env}-sfn-batch-orchestrator-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "states.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })

  tags = {
    Project = var.project
    Env     = var.env
  }
}

resource "aws_iam_role_policy" "sfn_batch_orchestrator" {
  name = "batch-orchestrator-run-task"
  role = aws_iam_role.sfn_batch_orchestrator.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["ecs:RunTask"]
        Resource = replace(aws_ecs_task_definition.batch.arn, "/:\\d+$/", ":*")
      },
      {
        # RunTask.sync統合の実行状態監視・実行キャンセルに必要
        Effect   = "Allow"
        Action   = ["ecs:StopTask", "ecs:DescribeTasks"]
        Resource = "*"
      },
      {
        # RunTask.sync統合がECS Task State ChangeイベントをEventBridge経由で受け取るために必要な、
        # AWS管理ルール（StepFunctionsGetEventsForECSTaskRule）固定のパーミッション
        Effect   = "Allow"
        Action   = ["events:PutTargets", "events:PutRule", "events:DescribeRule"]
        Resource = "arn:aws:events:${var.aws_region}:${data.aws_caller_identity.current.account_id}:rule/StepFunctionsGetEventsForECSTaskRule"
      },
      {
        Effect = "Allow"
        Action = ["iam:PassRole"]
        Resource = [
          aws_iam_role.task_execution.arn,
          aws_iam_role.task.arn
        ]
      },
      {
        # 実行履歴をCloudWatch Logsへ配信するために必要（ログ配信APIはリソースレベル権限非対応のためResource="*"がAWSの標準要件）
        Effect = "Allow"
        Action = [
          "logs:CreateLogDelivery",
          "logs:GetLogDelivery",
          "logs:UpdateLogDelivery",
          "logs:DeleteLogDelivery",
          "logs:ListLogDeliveries",
          "logs:PutResourcePolicy",
          "logs:DescribeResourcePolicies",
          "logs:DescribeLogGroups"
        ]
        Resource = "*"
      }
    ]
  })
}

resource "aws_cloudwatch_log_group" "sfn_batch_orchestrator" {
  name              = "/aws/vendedlogs/states/${var.project}-${var.env}-batch-orchestrator"
  retention_in_days = 30

  tags = {
    Project = var.project
    Env     = var.env
  }
}

locals {
  # 3つのTask stateで共通のRunTask.syncパラメータ（差分はOverrides.Command/Nextのみ）
  batch_run_task_parameters = {
    Cluster        = aws_ecs_cluster.this.arn
    TaskDefinition = aws_ecs_task_definition.batch.family
    LaunchType     = "FARGATE"
    NetworkConfiguration = {
      AwsvpcConfiguration = {
        Subnets        = var.batch_private_subnet_ids
        SecurityGroups = [aws_security_group.ecs.id]
        AssignPublicIp = "DISABLED"
      }
    }
  }

  # RunTask API呼び出し自体の一時的なエラー（スロットリング等）のみリトライ対象とする。
  # コンテナの非0終了（States.TaskFailed）はJob側の失敗であり、
  # 「前段が失敗したら後段を実行しない」という設計意図通りリトライせず即座に実行を止める。
  batch_run_task_retry = [
    {
      ErrorEquals     = ["ECS.AmazonECSException", "States.Timeout"]
      IntervalSeconds = 30
      MaxAttempts     = 2
      BackoffRate     = 2.0
    }
  ]
}

resource "aws_sfn_state_machine" "batch_orchestrator" {
  name     = "${var.project}-${var.env}-batch-orchestrator"
  role_arn = aws_iam_role.sfn_batch_orchestrator.arn
  type     = "STANDARD"

  definition = jsonencode({
    Comment = "日次売上集計バッチ: paymentIntakeJob → salesAggregationJob → settlementExportJob を順に実行し、前段が失敗したら後段を実行しない"
    StartAt = "PaymentIntakeJob"
    # バッチウィンドウ（14.6節: 02:00-05:00 JST = 3時間）全体を実行全体の安全網として設定
    TimeoutSeconds = 10800
    States = {
      PaymentIntakeJob = {
        Type     = "Task"
        Resource = "arn:aws:states:::ecs:runTask.sync"
        Parameters = merge(local.batch_run_task_parameters, {
          Overrides = {
            ContainerOverrides = [
              { Name = "batch", Command = ["--job=paymentIntakeJob"] }
            ]
          }
        })
        Retry = local.batch_run_task_retry
        Next  = "SalesAggregationJob"
      }
      SalesAggregationJob = {
        Type     = "Task"
        Resource = "arn:aws:states:::ecs:runTask.sync"
        Parameters = merge(local.batch_run_task_parameters, {
          Overrides = {
            ContainerOverrides = [
              { Name = "batch", Command = ["--job=salesAggregationJob"] }
            ]
          }
        })
        Retry = local.batch_run_task_retry
        Next  = "SettlementExportJob"
      }
      SettlementExportJob = {
        Type     = "Task"
        Resource = "arn:aws:states:::ecs:runTask.sync"
        Parameters = merge(local.batch_run_task_parameters, {
          Overrides = {
            ContainerOverrides = [
              { Name = "batch", Command = ["--job=settlementExportJob"] }
            ]
          }
        })
        Retry = local.batch_run_task_retry
        End   = true
      }
    }
  })

  logging_configuration {
    log_destination        = "${aws_cloudwatch_log_group.sfn_batch_orchestrator.arn}:*"
    include_execution_data = true
    level                  = "ERROR"
  }

  tags = {
    Project = var.project
    Env     = var.env
  }
}

# EventBridge Scheduler が Step Functions ステートマシンを起動するためのロール
resource "aws_iam_role" "batch_scheduler" {
  name = "${var.project}-${var.env}-batch-scheduler-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "scheduler.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })

  tags = {
    Project = var.project
    Env     = var.env
  }
}

resource "aws_iam_role_policy" "batch_scheduler" {
  name = "batch-start-orchestrator-execution"
  role = aws_iam_role.batch_scheduler.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["states:StartExecution"]
        Resource = aws_sfn_state_machine.batch_orchestrator.arn
      }
    ]
  })
}

# 日次売上集計バッチの起動スケジュール
resource "aws_scheduler_schedule" "batch_daily" {
  name       = "${var.project}-${var.env}-batch-daily"
  group_name = "default"

  flexible_time_window {
    mode = "OFF"
  }

  # スリープ中はNAT・Redisが無くバッチが必ず失敗するため、スケジュールごと止める
  state = var.batch_schedule_enabled ? "ENABLED" : "DISABLED"

  schedule_expression          = var.batch_schedule_expression
  schedule_expression_timezone = "UTC"

  target {
    arn      = aws_sfn_state_machine.batch_orchestrator.arn
    role_arn = aws_iam_role.batch_scheduler.arn
  }
}

# ---------------------------------------------------------------------------
# ECS Task Definition - DB Debug（psqlでのDB確認用。一時利用）
# ---------------------------------------------------------------------------
resource "aws_cloudwatch_log_group" "db_debug" {
  name              = "/ecs/${var.project}-${var.env}/db-debug"
  retention_in_days = 7

  tags = {
    Project = var.project
    Env     = var.env
  }
}

resource "aws_ecs_task_definition" "db_debug" {
  family                   = "${var.project}-${var.env}-db-debug"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "256"
  memory                   = "512"
  execution_role_arn       = aws_iam_role.task_execution.arn
  task_role_arn            = aws_iam_role.task.arn

  container_definitions = jsonencode([
    {
      name      = "psql"
      image     = "postgres:16-alpine"
      essential = true

      # ECS Execでシェル接続してpsqlを手動実行するためコンテナを起動させ続ける
      command = ["sleep", "infinity"]

      environment = [
        { name = "PGHOST", value = var.db_host },
        { name = "PGPORT", value = "5432" },
        { name = "PGDATABASE", value = var.db_name },
      ]

      secrets = [
        {
          name      = "PGUSER"
          valueFrom = "${var.db_password_secret_arn}:username::"
        },
        {
          name      = "PGPASSWORD"
          valueFrom = "${var.db_password_secret_arn}:password::"
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.db_debug.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "db-debug"
        }
      }
    }
  ])

  tags = {
    Project = var.project
    Env     = var.env
  }
}

# ---------------------------------------------------------------------------
# ECS Task Definition - Frontend（Next.js。backendと同じクラスタだが別ALB・別サービス）
# ---------------------------------------------------------------------------
resource "aws_cloudwatch_log_group" "frontend" {
  name              = "/ecs/${var.project}-${var.env}/frontend"
  retention_in_days = 30

  tags = {
    Project = var.project
    Env     = var.env
  }
}

resource "aws_ecs_task_definition" "frontend" {
  family                   = "${var.project}-${var.env}-frontend"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.frontend_cpu
  memory                   = var.frontend_memory
  execution_role_arn       = aws_iam_role.task_execution.arn
  task_role_arn            = aws_iam_role.task.arn

  container_definitions = jsonencode([
    {
      name      = "frontend"
      image     = "${var.frontend_image_url}:${var.frontend_image_tag}"
      essential = true

      stopTimeout = 30

      portMappings = [{
        containerPort = 3000
        protocol      = "tcp"
      }]

      # NEXT_PUBLIC_API_BASE_URLはビルド時にクライアントバンドルへ埋め込まれるため、
      # 実行時の環境変数はここでは渡さない(frontend/buildspec.ymlのビルド引数参照)
      environment = []

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = aws_cloudwatch_log_group.frontend.name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "frontend"
        }
      }

      healthCheck = {
        command     = ["CMD-SHELL", "wget -qO- http://localhost:3000/ || exit 1"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 30
      }
    }
  ])

  tags = {
    Project = var.project
    Env     = var.env
  }
}

# ---------------------------------------------------------------------------
# ECS Service
# ---------------------------------------------------------------------------
resource "aws_ecs_service" "app" {
  name            = "${var.project}-${var.env}-app-service"
  cluster         = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.app.arn
  desired_count   = var.desired_count
  launch_type     = "FARGATE"

  # デプロイ設定（ローリングアップデート）
  deployment_minimum_healthy_percent = 50
  deployment_maximum_percent         = 200

  # 新タスクがヘルスチェックに失敗し続ける場合、自動的に直前のタスク定義へロールバック
  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  # アプリ起動が完了する前にALBヘルスチェック失敗でタスクが強制終了されないよう、起動猶予期間を設ける
  # LB未接続(スリープ中)のサービスには猶予期間を設定できないため0にする
  health_check_grace_period_seconds = var.attach_load_balancer ? var.health_check_grace_period_seconds : 0

  # タスクをAZ間で常に均等配置（1AZ1タスク）に保つ
  availability_zone_rebalancing = "ENABLED"

  network_configuration {
    subnets          = var.private_subnet_ids
    security_groups  = [aws_security_group.ecs.id]
    assign_public_ip = false
  }

  # スリープ中(ALB削除中)はターゲットグループがLBに紐付かず、その状態でload_balancerを指定したまま
  # UpdateServiceすると「does not have an associated load balancer」で失敗するため、LB接続ごと外す。
  # (load_balancerはin-placeで変更可能。外す場合は空リストで更新される)
  dynamic "load_balancer" {
    for_each = var.attach_load_balancer ? [1] : []
    content {
      target_group_arn = var.target_group_arn
      container_name   = "app"
      container_port   = 8080
    }
  }

  # task_definitionは無視しない: タスク定義のイメージは現在デプロイ済みのものを読み取って
  # 組み立てるため、Terraformのリビジョンへ切り替えてもイメージは変わらず設定変更だけが反映される。
  # desired_countはオートスケーリングが変更するため無視する。
  lifecycle {
    ignore_changes = [desired_count]
  }

  depends_on = [aws_iam_role_policy_attachment.task_execution]

  tags = {
    Project = var.project
    Env     = var.env
  }
}

# ---------------------------------------------------------------------------
# Auto Scaling
# ---------------------------------------------------------------------------
resource "aws_appautoscaling_target" "ecs" {
  max_capacity       = var.max_capacity
  min_capacity       = var.min_capacity
  resource_id        = "service/${aws_ecs_cluster.this.name}/${aws_ecs_service.app.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "cpu" {
  name               = "${var.project}-${var.env}-cpu-scaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.ecs.resource_id
  scalable_dimension = aws_appautoscaling_target.ecs.scalable_dimension
  service_namespace  = aws_appautoscaling_target.ecs.service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
    target_value       = 70.0
    scale_in_cooldown  = 300
    scale_out_cooldown = 60
  }
}

# ---------------------------------------------------------------------------
# ECS Service - Frontend
# ---------------------------------------------------------------------------
resource "aws_ecs_service" "frontend" {
  name            = "${var.project}-${var.env}-frontend-service"
  cluster         = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.frontend.arn
  desired_count   = var.frontend_desired_count
  launch_type     = "FARGATE"

  deployment_minimum_healthy_percent = 50
  deployment_maximum_percent         = 200

  deployment_circuit_breaker {
    enable   = true
    rollback = true
  }

  # LB未接続(スリープ中)のサービスには猶予期間を設定できないため0にする
  health_check_grace_period_seconds = var.attach_load_balancer ? var.health_check_grace_period_seconds : 0

  availability_zone_rebalancing = "ENABLED"

  network_configuration {
    subnets          = var.private_subnet_ids
    security_groups  = [aws_security_group.ecs.id]
    assign_public_ip = false
  }

  # スリープ中(ALB削除中)はターゲットグループがLBに紐付かず、その状態でload_balancerを指定したまま
  # UpdateServiceすると「does not have an associated load balancer」で失敗するため、LB接続ごと外す。
  # (load_balancerはin-placeで変更可能。外す場合は空リストで更新される)
  dynamic "load_balancer" {
    for_each = var.attach_load_balancer ? [1] : []
    content {
      target_group_arn = var.frontend_target_group_arn
      container_name   = "frontend"
      container_port   = 3000
    }
  }

  # CodePipelineによるデプロイ時にタスク定義が更新されるため
  # Terraformがdesired_countやtask_definitionを上書きしないよう無視
  lifecycle {
    ignore_changes = [task_definition, desired_count]
  }

  depends_on = [aws_iam_role_policy_attachment.task_execution]

  tags = {
    Project = var.project
    Env     = var.env
  }
}

resource "aws_appautoscaling_target" "frontend" {
  max_capacity       = var.frontend_max_capacity
  min_capacity       = var.frontend_min_capacity
  resource_id        = "service/${aws_ecs_cluster.this.name}/${aws_ecs_service.frontend.name}"
  scalable_dimension = "ecs:service:DesiredCount"
  service_namespace  = "ecs"
}

resource "aws_appautoscaling_policy" "frontend_cpu" {
  name               = "${var.project}-${var.env}-frontend-cpu-scaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.frontend.resource_id
  scalable_dimension = aws_appautoscaling_target.frontend.scalable_dimension
  service_namespace  = aws_appautoscaling_target.frontend.service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "ECSServiceAverageCPUUtilization"
    }
    target_value       = 70.0
    scale_in_cooldown  = 300
    scale_out_cooldown = 60
  }
}
