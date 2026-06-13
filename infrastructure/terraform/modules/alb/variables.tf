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
  description = "ALBを配置するパブリックサブネットID(2つ以上推奨)"
  type        = list(string)
}

variable "security_group_ids" {
  description = "ALBに適用するSG(alb-sg)"
  type        = list(string)
}

variable "target_port" {
  description = "ターゲット(EC2/ECS)のリスニングポート"
  type        = number
  default     = 8080
}

variable "health_check_path" {
  description = "ALBヘルスチェックのパス"
  type        = string
  default     = "/"
}

variable "target_instance_ids" {
  description = "EC2インスタンスID(暫定運用時のみ使用、ECS移行後はaws_lb_target_group_attachmentをECSサービス側で管理)"
  type        = list(string)
  default     = []
}
