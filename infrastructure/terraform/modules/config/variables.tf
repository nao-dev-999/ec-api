variable "project" {
  type = string
}

variable "env" {
  type = string
}

variable "notification_emails" {
  description = "AWS Configのコンプライアンス違反通知(SNS)を受け取るメールアドレス一覧。サブスクライブには各アドレスでの確認メール承認が必要"
  type        = list(string)
  default     = []
}

variable "config_log_retention_days" {
  description = "AWS Config記録データの保持日数。コスト削減のため、経過後は自動削除する。"
  type        = number
  default     = 365
}
