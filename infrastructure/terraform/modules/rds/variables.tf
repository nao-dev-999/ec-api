variable "project" {
  description = "Project name"
  type        = string
}

variable "env" {
  description = "Environment name"
  type        = string
}

variable "identifier" { # cluster_identifier から identifier に変更
  description = "RDS instance identifier"
  type        = string
}

variable "engine" {
  description = "Database engine"
  type        = string
}

variable "engine_version" {
  description = "Database engine version"
  type        = string
}

variable "instance_class" {
  description = "RDS instance class"
  type        = string
}

variable "database_name" {
  description = "Name of the database to create"
  type        = string
}

variable "master_username" {
  description = "Master username for the database"
  type        = string
}

variable "subnet_ids" {
  description = "List of private subnet IDs for the DB subnet group"
  type        = list(string)
}

variable "security_group_ids" {
  description = "List of security group IDs for the RDS instance"
  type        = list(string)
}