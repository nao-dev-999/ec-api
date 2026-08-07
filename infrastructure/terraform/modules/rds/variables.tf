variable "project" {
  description = "Project name"
  type        = string
}

variable "env" {
  description = "Environment name"
  type        = string
}

variable "identifier" {
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

variable "vpc_id" {
  type = string
}

variable "ecs_sg_id" {
  type = string
}

variable "backup_retention_period" {
  description = "自動バックアップの保持日数（0で無効。デフォルトのTerraform/AWS挙動は0=バックアップ無効のため明示的に設定する）"
  type        = number
  default     = 7
}

variable "max_allocated_storage" {
  description = "ストレージ自動拡張の上限（GB）。allocated_storageに近づくと自動的にスケールアップする"
  type        = number
  default     = 100
}
