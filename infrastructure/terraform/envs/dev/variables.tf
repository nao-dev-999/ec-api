variable "project" {}
variable "env" {}
variable "aws_region" {}

# スリープモード(コスト削減)
variable "sleep_mode" {
  type        = bool
  default     = false
  description = "trueにすると時間課金のリソース(NAT Gateway・ALB・WAF・Redis等)を削除・停止してコストを抑える(destroyせずに済む)。falseで元に戻る"
}

# VPC
variable "vpc_cidr" {}
variable "public_subnet_cidrs" { type = list(string) }
variable "private_subnet_cidrs" { type = list(string) }
variable "availability_zones" { type = list(string) }
variable "nat_gateway_count" {
  type        = number
  default     = 1
  description = "NAT Gatewayの数。1（全AZ共有）または length(availability_zones)（AZごとに1台）を指定する"
}

# RDS
variable "rds_engine" { default = "postgres" }
variable "rds_engine_version" { default = "16" }
variable "rds_instance_class" { default = "db.t4g.micro" }
variable "rds_database_name" {}
variable "rds_master_username" {}
variable "rds_multi_az" {
  type    = bool
  default = false
}
variable "rds_skip_final_snapshot" {
  type    = bool
  default = true
}
variable "rds_deletion_protection" {
  type    = bool
  default = false
}

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

# Swagger UI
variable "swagger_enabled" {
  description = "Swagger UI(/swagger-ui, /v3/api-docs)をこの環境で公開するか"
  type        = bool
  default     = false
}

# CodePipeline
variable "manual_approval_enabled" {
  description = "CodePipelineのDeploy(バッチはタスク定義反映)前に手動承認ステージを挟むか"
  type        = bool
  default     = false
}
