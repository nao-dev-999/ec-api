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

  db_host     = module.rds.rds_endpoint
  db_name     = var.rds_database_name
  db_username = var.rds_master_username
  db_password_secret_arn = module.rds.rds_secret_arn

  task_cpu      = "512"
  task_memory   = "1024"
  desired_count = 1
  min_capacity  = 1
  max_capacity  = 4
}

# CodePipeline
module "codepipeline" {
  source = "../../modules/codepipeline"

  project    = var.project
  env        = var.env
  aws_region = var.aws_region

  github_repository       = var.github_repository
  github_branch           = var.github_branch

  app_repository_url = module.ecr.app_repository_url
  ecs_cluster_name   = module.ecs.cluster_name
  ecs_service_name   = module.ecs.service_name

  task_execution_role_arn = module.ecs.task_execution_role_arn
  task_role_arn           = module.ecs.task_role_arn

  flyway_task_definition_family = module.ecs.flyway_task_definition_family
  flyway_subnet_id              = module.vpc.private_subnet_ids[0]
  flyway_sg_id                  = module.ecs.ecs_sg_id
}
