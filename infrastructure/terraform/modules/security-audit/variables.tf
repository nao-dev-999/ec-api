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
