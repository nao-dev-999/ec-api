variable "project" { type = string }
variable "env" { type = string }
variable "aws_region" { type = string }

variable "vpc_id" { type = string }
variable "private_subnet_ids" { type = list(string) }
variable "alb_sg_id" { type = string }
variable "target_group_arn" { type = string }

variable "app_image_url" { type = string }
variable "app_image_tag" {
  type    = string
  default = "latest"
}

variable "flyway_image_url" { type = string }
variable "flyway_image_tag" {
  type    = string
  default = "latest"
}

variable "db_host" { type = string }
variable "db_name" { type = string }
variable "db_password_secret_arn" {
  type      = string
  description = "RDS managed Secrets Manager ARN"
}

variable "task_cpu" {
  type    = string
  default = "512"
}

variable "task_memory" {
  type    = string
  default = "1024"
}

variable "desired_count" {
  type    = number
  default = 1
}

variable "min_capacity" {
  type    = number
  default = 1
}

variable "max_capacity" {
  type    = number
  default = 4
}

variable "batch_image_url" { type = string }
variable "batch_image_tag" {
  type    = string
  default = "latest"
}

variable "batch_cpu" {
  type    = string
  default = "1024"
}

variable "batch_memory" {
  type    = string
  default = "2048"
}

variable "batch_private_subnet_ids" {
  description = "EventBridge SchedulerがRunTaskするFargateタスクを配置するサブネット（private_subnet_idsと同一でよい）"
  type        = list(string)
}

variable "batch_schedule_expression" {
  description = "日次集計バッチの起動cron式（デフォルト: JST 02:00 = UTC 17:00）"
  type        = string
  default     = "cron(0 17 * * ? *)"
}
