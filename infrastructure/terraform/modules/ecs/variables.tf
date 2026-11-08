variable "project" { type = string }
variable "env" { type = string }
variable "aws_region" { type = string }

variable "vpc_id" { type = string }
variable "private_subnet_ids" { type = list(string) }
variable "alb_sg_id" { type = string }
variable "target_group_arn" { type = string }
variable "frontend_alb_sg_id" { type = string }

variable "app_image_url" { type = string }
variable "app_image_tag" {
  type        = string
  default     = null
  description = "appイメージのタグ。null(既定)の場合は現在デプロイ済みのイメージをAWSから読み取って使う。初回構築時(読み取り元が存在しない)のみ明示指定する。"
}

variable "flyway_image_url" { type = string }
variable "flyway_image_tag" {
  type    = string
  default = "latest"
}

variable "db_host" { type = string }
variable "db_name" { type = string }
variable "db_password_secret_arn" {
  type        = string
  description = "RDS managed Secrets Manager ARN"
}

variable "swagger_enabled" {
  type        = bool
  default     = false
  description = "Swagger UI(/swagger-ui, /v3/api-docs)をこの環境で公開するか。安全側のデフォルトとして無効。"
}

variable "frontend_public_url" {
  type        = string
  description = "frontendのALB公開URL(https://...)。backendコンテナのCORS許可オリジン(APP_CORS_ALLOWED_ORIGINS)に設定する。"
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
  type        = string
  default     = null
  description = "batchイメージのタグ。null(既定)の場合は現在デプロイ済みのイメージをAWSから読み取って使う。初回構築時(読み取り元が存在しない)のみ明示指定する。"
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

variable "batch_schedule_enabled" {
  type        = bool
  default     = true
  description = "falseにすると日次バッチのスケジュールを無効化する(スリープ中はNAT・Redisが無く必ず失敗するため)"
}

variable "health_check_grace_period_seconds" {
  type    = number
  default = 120
}

variable "enable_redis" {
  type        = bool
  default     = true
  description = "falseにするとElastiCache Redisレプリケーショングループを削除する(SG・サブネットグループ・AUTHトークンは残す。スリープ用)"
}

variable "redis_node_type" {
  type    = string
  default = "cache.t3.micro"
}

variable "redis_num_cache_clusters" {
  type    = number
  default = 1
}

variable "redis_notification_topic_arn" {
  type    = string
  default = null
}

variable "frontend_image_url" { type = string }
variable "frontend_image_tag" {
  type    = string
  default = "latest"
}

variable "frontend_target_group_arn" { type = string }

variable "frontend_cpu" {
  type    = string
  default = "256"
}

variable "frontend_memory" {
  type    = string
  default = "512"
}

variable "frontend_desired_count" {
  type    = number
  default = 1
}

variable "frontend_min_capacity" {
  type    = number
  default = 1
}

variable "frontend_max_capacity" {
  type    = number
  default = 2
}
