variable "project" {
  type = string
}

variable "env" {
  type = string
}

variable "sns_topic_arn" {
  description = "GuardDutyの高深刻度検出（severity>=7）通知先SNSトピックARN。イベントブリッジからの発行を許可するトピックポリシーが設定済みであること"
  type        = string
}

variable "cloudtrail_log_retention_days" {
  description = "CloudTrailログの保持日数。監査・侵入調査に使うため長めに設定。コンプライアンス要件があればその日数に合わせて見直すこと。"
  type        = number
  default     = 365
}
