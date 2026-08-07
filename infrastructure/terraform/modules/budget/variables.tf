variable "project" {
  type = string
}

variable "env" {
  type = string
}

variable "monthly_limit_usd" {
  description = "月次コスト予算の上限（USD）"
  type        = string
  default     = "50"
}

variable "notification_emails" {
  description = "予算超過通知を受け取るメールアドレス一覧"
  type        = list(string)
  default     = []
}
