variable "project" {
  type = string
}

variable "env" {
  type = string
}

variable "vpc_id" {
  type = string
}

variable "public_subnet_ids" {
  type        = list(string)
}

variable "target_port" {
  type        = number
  default     = 8080
}

variable "health_check_path" {
  type        = string
  default     = "/actuator/health"
}

variable "maintenance_mode_enabled" {
  type        = bool
  default     = false
  description = "true にすると、ALBリスナーが全リクエストを固定レスポンス(503)で返す。大規模な計画停止など、アプリ自体を起動しておけない場合に使う（アプリ内のMaintenanceFilterはアプリが起動している前提のため代替できない）。"
}

variable "enable_deletion_protection" {
  type        = bool
  default     = false
  description = "ALBの削除保護。誤ってterraform destroy/applyでALBを消してしまうことを防ぐ。devでは頻繁に作り直すためfalse、本番環境ではtrueにすること。"
}

variable "deregistration_delay" {
  type        = number
  default     = 30
  description = "ターゲット登録解除までの待機秒数（デフォルトは300秒）。ECS Fargateのローリングデプロイを速くするため短縮。ECSタスク定義のstopTimeout・アプリのグレースフルシャットダウン時間との整合を取ること。"
}

variable "idle_timeout" {
  type        = number
  default     = 60
  description = "クライアント/ターゲットとの接続がアイドル状態を保てる秒数。アプリ側のkeepaliveタイムアウトはこの値より長く設定すること（短いと接続の使い回し時にコネクションリセットが発生しうる）。"
}

variable "enable_waf_fail_open" {
  type        = bool
  default     = false
  description = "WAFが応答不能な場合の挙動。false(fail closed)はセキュリティ優先でリクエストを拒否、trueは可用性優先でWAF未検査のままリクエストを通す。"
}

variable "access_log_expiration_days" {
  type        = number
  default     = 365
  description = "ALBアクセスログの保持日数。30日でIA、90日でGlacierへ移行後、この日数で自動削除する。"
}

