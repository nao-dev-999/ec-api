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
    description     = "Allow from ALB"
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [var.alb_sg_id]
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

resource "aws_elasticache_cluster" "this" {
  cluster_id           = "${var.project}-${var.env}-redis"
  engine               = "redis"
  node_type            = "cache.t3.micro"
  num_cache_nodes      = 1
  parameter_group_name = "default.redis7"
  engine_version       = "7.1"
  port                 = 6379
  subnet_group_name    = aws_elasticache_subnet_group.this.name
  security_group_ids   = [aws_security_group.redis.id]

  tags = {
    Name    = "${var.project}-${var.env}-redis"
    Project = var.project
    Env     = var.env
  }
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
      image     = "${var.app_image_url}:${var.app_image_tag}"
      essential = true

      portMappings = [{
        containerPort = 8080
        protocol      = "tcp"
      }]

      environment = [
        { name = "SPRING_PROFILES_ACTIVE", value = var.env },
        { name = "SPRING_DATASOURCE_URL", value = "jdbc:postgresql://${var.db_host}:5432/${var.db_name}" },
        { name = "SPRING_DATA_REDIS_HOST", value = aws_elasticache_cluster.this.cache_nodes[0].address },
        { name = "SPRING_DATA_REDIS_PORT", value = "6379" },
        # Flywayはアプリ起動時には無効化（別タスクで実行）
        { name = "SPRING_FLYWAY_ENABLED", value = "false" },
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
      name      = "flyway"
      # Flyway公式CLIイメージ（マイグレーションSQLを同梱してビルド）を使用
      image     = "${var.flyway_image_url}:${var.flyway_image_tag}"
      essential = true

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
      image     = "${var.batch_image_url}:${var.batch_image_tag}"
      essential = true

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

  tags = {
    Project = var.project
    Env     = var.env
  }
}

# EventBridge Scheduler が ecs:RunTask を呼び出すためのロール
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
  name = "batch-run-task"
  role = aws_iam_role.batch_scheduler.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["ecs:RunTask"]
        Resource = replace(aws_ecs_task_definition.batch.arn, "/:\\d+$/", ":*")
      },
      {
        Effect = "Allow"
        Action = ["iam:PassRole"]
        Resource = [
          aws_iam_role.task_execution.arn,
          aws_iam_role.task.arn
        ]
      }
    ]
  })
}

# 日次売上集計バッチの起動スケジュール（14.6節: バッチウィンドウ 02:00〜05:00 JST）
resource "aws_scheduler_schedule" "batch_daily" {
  name       = "${var.project}-${var.env}-batch-daily"
  group_name = "default"

  flexible_time_window {
    mode = "OFF"
  }

  schedule_expression          = var.batch_schedule_expression
  schedule_expression_timezone = "UTC"

  target {
    arn      = aws_ecs_cluster.this.arn
    role_arn = aws_iam_role.batch_scheduler.arn

    ecs_parameters {
      # revisionを固定せず family名を渡すことで、CodeBuildが register-task-definition で
      # 新リビジョンを登録するたびTerraform再applyなしで自動的に最新版を実行する
      task_definition_arn = aws_ecs_task_definition.batch.family
      launch_type         = "FARGATE"

      network_configuration {
        subnets          = var.batch_private_subnet_ids
        security_groups  = [aws_security_group.ecs.id]
        assign_public_ip = false
      }
    }
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
  health_check_grace_period_seconds = 120

  # タスクをAZ間で常に均等配置（1AZ1タスク）に保つ
  availability_zone_rebalancing = "ENABLED"

  network_configuration {
    subnets          = var.private_subnet_ids
    security_groups  = [aws_security_group.ecs.id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = var.target_group_arn
    container_name   = "app"
    container_port   = 8080
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
