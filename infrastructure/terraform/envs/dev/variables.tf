variable "project" {}
variable "env" {}
variable "aws_region" {}

# VPC
variable "vpc_cidr" {}
variable "public_subnet_cidrs" { type = list(string) }
variable "private_subnet_cidrs" { type = list(string) }
variable "availability_zones" { type = list(string) }

# RDS
variable "rds_engine" { default = "postgres" }
variable "rds_engine_version" { default = "16" }
variable "rds_instance_class" { default = "db.t4g.micro" }
variable "rds_database_name" {}
variable "rds_master_username" {}

# GitHub / CodePipeline
variable "github_repository" {
  description = "owner/repo 形式"
}
variable "github_branch" {
  default = "main"
}
