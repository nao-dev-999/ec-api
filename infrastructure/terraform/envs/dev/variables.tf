variable "project" {}
variable "env" {}
variable "aws_region" {}

# VPC
variable "vpc_cidr" {}
variable "public_subnet_cidrs" { type = list(string) }
variable "private_subnet_cidrs" { type = list(string) }
variable "availability_zones" { type = list(string) }

# RDS
variable "rds_engine" {}
variable "rds_engine_version" {}
variable "rds_instance_class" {}
variable "rds_database_name" {}
variable "rds_master_username" {}

# EC2
variable "ec2_ami_id" {}
variable "ec2_instance_type" {}
variable "key_name" { default = null } # 追加

