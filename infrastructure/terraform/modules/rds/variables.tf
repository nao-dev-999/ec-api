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

variable "aws_region" {
  description = "RDS自動起動検知(auto_restop)のEventBridge/SSM Automation ARN組み立てに使用"
  type        = string
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

variable "multi_az" {
  description = "検証環境: false / 本番環境: true"
  type        = bool
  default     = false
}

variable "skip_final_snapshot" {
  description = "検証環境: true / 本番環境: false"
  type        = bool
  default     = true
}

variable "deletion_protection" {
  description = "検証環境: false / 本番環境: true"
  type        = bool
  default     = false
}

variable "alarm_sns_topic_arn" {
  description = "RDSイベント通知(aws_db_event_subscription)の送信先SNSトピックARN"
  type        = string
}

variable "instance_state" {
  type        = string
  default     = null
  description = "インスタンスの状態(\"available\" または \"stopped\")。nullの場合はTerraformで起動/停止を管理しない(検証環境のスリープ用)"

  validation {
    condition     = var.instance_state == null || contains(["available", "stopped"], coalesce(var.instance_state, "available"))
    error_message = "instance_state は null、\"available\"、\"stopped\" のいずれかを指定してください。"
  }
}
