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

