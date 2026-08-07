# VPC Module
module "vpc" {
  source = "../../modules/vpc"

  project              = var.project
  env                  = var.env
  vpc_cidr             = var.vpc_cidr
  public_subnet_cidrs  = var.public_subnet_cidrs
  private_subnet_cidrs = var.private_subnet_cidrs
  availability_zones   = var.availability_zones
}

# RDS Module (db.t4g.nano)
module "rds" {
  source = "../../modules/rds" # 既に変更済み
  project            = var.project
  env                = var.env
  identifier         = "${var.project}-${var.env}-rds"
  engine             = var.rds_engine
  engine_version     = var.rds_engine_version
  instance_class     = var.rds_instance_class
  database_name      = var.rds_database_name
  master_username    = var.rds_master_username
  vpc_id             = module.vpc.vpc_id
  subnet_ids         = module.vpc.private_subnet_ids
  ecs_sg_id          = module.ecs.ecs_sg_id
}

# ALB
module "alb" {
  source = "../../modules/alb"

  project             = var.project
  env                 = var.env
  vpc_id              = module.vpc.vpc_id
  public_subnet_ids   = module.vpc.public_subnet_ids
  target_port         = 8080
  health_check_path   = "/actuator/health"
}

# WAF（ALBの手前でIPアドレス単位のレートベース制限）
module "waf" {
  source = "../../modules/waf"

  project = var.project
  env     = var.env
  alb_arn = module.alb.alb_arn
}

# ECR
module "ecr" {
  source = "../../modules/ecr"
  project = var.project
  env     = var.env
}

# ECS (Fargate + ElastiCache Redis)
module "ecs" {
  source = "../../modules/ecs"

  project    = var.project
  env        = var.env
  aws_region = var.aws_region

  vpc_id             = module.vpc.vpc_id
  private_subnet_ids = module.vpc.private_subnet_ids
  alb_sg_id          = module.alb.alb_sg_id
  target_group_arn   = module.alb.target_group_arn

  app_image_url = module.ecr.app_repository_url
  app_image_tag = "latest"

  flyway_image_url = module.ecr.flyway_repository_url
  flyway_image_tag = "latest"

  db_host                = module.rds.rds_endpoint
  db_name                = var.rds_database_name
  db_password_secret_arn = module.rds.rds_secret_arn

  task_cpu      = "512"
  task_memory   = "1024"
  desired_count = 2
  min_capacity  = 2
  max_capacity  = 4

  batch_image_url          = module.ecr.batch_repository_url
  batch_image_tag          = "latest"
  batch_private_subnet_ids = module.vpc.private_subnet_ids
}

# CodePipeline
module "codepipeline" {
  source = "../../modules/codepipeline"

  project    = var.project
  env        = var.env
  aws_region = var.aws_region

  github_repository       = var.github_repository
  github_branch           = var.github_branch

  app_repository_url    = module.ecr.app_repository_url
  flyway_repository_url = module.ecr.flyway_repository_url
  ecs_cluster_name      = module.ecs.cluster_name
  ecs_service_name      = module.ecs.service_name

  task_execution_role_arn = module.ecs.task_execution_role_arn
  task_role_arn           = module.ecs.task_role_arn

  flyway_task_definition_family = module.ecs.flyway_task_definition_family
  flyway_subnet_id              = module.vpc.private_subnet_ids[0]
  flyway_sg_id                  = module.ecs.ecs_sg_id

  batch_repository_url         = module.ecr.batch_repository_url
  batch_task_definition_family = module.ecs.batch_task_definition_family
}

# AWS Config（設定ミス・非準拠状態の検知と通知）
# 他モジュールのデプロイフローに影響を与えないよう独立して追加
module "config" {
  source = "../../modules/config"

  project = var.project
  env     = var.env

  notification_emails = var.config_notification_emails
}

# CloudWatch Alarms（アプリのERRORログ / ECS起動数0 / CPU使用率90%以上 → SNS通知）
module "alarms" {
  source = "../../modules/alarms"

  project = var.project
  env     = var.env

  aws_region = var.aws_region

  ecs_cluster_name   = module.ecs.cluster_name
  ecs_service_name   = module.ecs.service_name
  app_log_group_name = module.ecs.app_log_group_name

  alb_arn_suffix              = module.alb.alb_arn_suffix
  alb_target_group_arn_suffix = module.alb.target_group_arn_suffix
  rds_instance_id             = module.rds.db_instance_id
  redis_cluster_id            = module.ecs.redis_cluster_id

  notification_emails = var.alarm_notification_emails
}

# セキュリティ監査基盤（CloudTrail: APIアクティビティ証跡 / GuardDuty: 脅威検知）
# 高深刻度のGuardDuty検出はAWS Configと同じ通知チャンネル（module.config）に集約する
module "security_audit" {
  source = "../../modules/security-audit"

  project = var.project
  env     = var.env

  sns_topic_arn = module.config.sns_topic_arn
}

# コスト監視（想定外課金の検知）
module "budget" {
  source = "../../modules/budget"

  project = var.project
  env     = var.env

  monthly_limit_usd   = var.monthly_budget_usd
  notification_emails = var.budget_notification_emails
}
