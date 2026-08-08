variable "project" {
  type = string
}

variable "env" {
  type = string
}

variable "aws_region" {
  description = "CloudWatchダッシュボードのウィジェットに指定するリージョン"
  type        = string
}

variable "ecs_cluster_name" {
  type = string
}

variable "ecs_service_name" {
  type = string
}

variable "app_log_group_name" {
  description = "ERRORログ検知の対象とするアプリのCloudWatch Logsロググループ名"
  type        = string
}

variable "alb_arn_suffix" {
  type = string
}

variable "alb_target_group_arn_suffix" {
  type = string
}

variable "rds_instance_id" {
  type = string
}

variable "redis_cluster_id" {
  type = string
}

variable "rds_database_connections_threshold" {
  description = "RDS DatabaseConnectionsアラームの閾値。インスタンスクラス変更時はmax_connectionsに応じて見直す（db.t3.microのmax_connections目安は約110）"
  type        = number
  default     = 80
}

variable "notification_emails" {
  description = "アラーム通知(SNS)を受け取るメールアドレス一覧。サブスクライブには各アドレスでの確認メール承認が必要"
  type        = list(string)
  default     = []
}

variable "waf_web_acl_metric_name" {
  description = "WAF WebACLのCloudWatchメトリクス名（AWS/WAFV2名前空間のWebACLディメンションに使用）"
  type        = string
}

variable "waf_auth_rate_limit_metric_name" {
  description = "WAF auth-rate-limitルールのCloudWatchメトリクス名（Ruleディメンションに使用）"
  type        = string
}

variable "waf_general_rate_limit_metric_name" {
  description = "WAF general-rate-limitルールのCloudWatchメトリクス名（Ruleディメンションに使用）"
  type        = string
}

variable "waf_blocked_requests_threshold" {
  description = "WAF全体(全ルール合計)のBlockedRequestsアラームの閾値（5分間の合計）"
  type        = number
  default     = 100
}

variable "waf_auth_rate_limit_threshold" {
  description = "WAF auth-rate-limitルールのBlockedRequestsアラームの閾値（5分間の合計）。ログイン・サインアップへのブルートフォース試行を検知する"
  type        = number
  default     = 1
}
