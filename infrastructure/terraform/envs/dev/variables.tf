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

# AWS Config
variable "config_notification_emails" {
  description = "AWS Configのコンプライアンス違反通知を受け取るメールアドレス一覧"
  type        = list(string)
  default     = []
}

# CloudWatch Alarms
variable "alarm_notification_emails" {
  description = "CloudWatchアラーム（ERRORログ / ECS起動数0 / CPU使用率90%以上）の通知を受け取るメールアドレス一覧"
  type        = list(string)
  default     = []
}

# AWS Budgets
variable "monthly_budget_usd" {
  description = "月次コスト予算の上限（USD）"
  type        = string
  default     = "50"
}

variable "budget_notification_emails" {
  description = "予算超過通知を受け取るメールアドレス一覧"
  type        = list(string)
  default     = []
}
