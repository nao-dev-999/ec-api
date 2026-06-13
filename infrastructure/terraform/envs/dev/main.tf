data "aws_secretsmanager_secret" "rds_password" {
  name = "${var.project}/${var.env}/rds-password"
}

data "aws_secretsmanager_secret_version" "rds_password" {
  secret_id = data.aws_secretsmanager_secret.rds_password.id
}

# 1. VPC Module
module "vpc" {
  source = "../../modules/vpc"

  project              = var.project
  env                  = var.env
  vpc_cidr             = var.vpc_cidr
  public_subnet_cidrs  = var.public_subnet_cidrs
  private_subnet_cidrs = var.private_subnet_cidrs
  availability_zones   = var.availability_zones
}

# 2. RDS Module (db.t4g.nano)
# module "rds" { # モジュール名を "rds" に変更
#   source = "../../modules/rds" # 既に変更済み
#
#   project            = var.project
#   env                = var.env
#   identifier         = "${var.project}-${var.env}-rds"
#
#   engine             = var.rds_engine
#   engine_version     = var.rds_engine_version
#   instance_class     = var.rds_instance_class
#   database_name      = var.rds_database_name
#   master_username    = var.rds_master_username
#   master_password    = var.rds_master_password
#
#   vpc_id             = module.vpc.vpc_id
#   subnet_ids         = module.vpc.private_subnet_ids
#   security_group_ids = [module.vpc.db_sg_id]
# }

# 3. EC2 Module
# module "ec2" {
#   source = "../../modules/ec2"
#
#   project          = var.project
#   env              = var.env
#   key_name         = var.key_name
#   ami_id           = var.ec2_ami_id
#   instance_type    = var.ec2_instance_type
#   subnet_id        = module.vpc.private_subnet_ids[0]
#   security_group_ids = [module.vpc.web_sg_id]
# }