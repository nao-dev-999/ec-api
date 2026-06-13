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
module "rds" { # モジュール名を "rds" に変更
  source = "../../modules/rds" # 既に変更済み
  project            = var.project
  env                = var.env
  identifier         = "${var.project}-${var.env}-rds"
  engine             = var.rds_engine
  engine_version     = var.rds_engine_version
  instance_class     = var.rds_instance_class
  database_name      = var.rds_database_name
  master_username    = var.rds_master_username
  subnet_ids         = module.vpc.private_subnet_ids
  security_group_ids = [module.vpc.db_sg_id]
}

# 3. EC2 Module
module "ec2" {
  source = "../../modules/ec2"

  project          = var.project
  env              = var.env
  ami_id           = var.ec2_ami_id
  instance_type    = var.ec2_instance_type
  subnet_id        = module.vpc.private_subnet_ids[0]
  security_group_ids = [module.vpc.web_sg_id]
}

module "alb" {
  source = "../../modules/alb"

  project             = var.project
  env                 = var.env
  vpc_id              = module.vpc.vpc_id
  public_subnet_ids   = module.vpc.public_subnet_ids
  security_group_ids  = [module.vpc.alb_sg_id]
  target_port         = 8080
  health_check_path   = "/"
  target_instance_ids = [module.ec2.instance_id]
}
