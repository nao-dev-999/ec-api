variable "project" {
  type = string
}

variable "env" {
  type = string
}

variable "alb_arn" {
  type        = string
  description = "レートベースルールを適用する ALB の ARN"
}

variable "general_rate_limit" {
  type        = number
  default     = 2000
  description = "IPアドレス単位のリクエスト上限（5分間のローリングウィンドウ、AWS WAFv2の最小値は100）。全エンドポイント向けの粗い足切り。細かい制限はアプリ側のFilterで行う。"
}

variable "auth_rate_limit" {
  type        = number
  default     = 300
  description = "ログイン・サインアップ系エンドポイント（/auth/login, /auth/signup を含むパス）向けのIP単位リクエスト上限（5分間）。ブルートフォース・スパムアカウント作成対策。"
}

variable "waf_log_retention_days" {
  type        = number
  default     = 30
  description = "WAFログを保存するS3バケットのオブジェクト保持日数。コスト削減のため、経過後は自動削除する。"
}

variable "managed_rules_count_mode" {
  type        = bool
  default     = false
  description = "true にすると、AWSマネージドルールグループ(Common/KnownBadInputs/SQLi/IPReputation)を検知のみ(カウント)で動作させ、実際のブロックは行わない。新規追加直後は誤検知(false positive)がないかWAFログ/CloudWatchで監視するため、一時的にtrueにすることを推奨。"
}
