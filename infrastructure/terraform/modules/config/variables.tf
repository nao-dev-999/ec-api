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
