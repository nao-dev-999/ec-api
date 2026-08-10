variable "project" { type = string }
variable "env" { type = string }
variable "vpc_cidr" { type = string }
variable "public_subnet_cidrs" { type = list(string) }
variable "private_subnet_cidrs" { type = list(string) }
variable "availability_zones" { type = list(string) }

variable "vpc_flow_log_retention_days" {
  description = "VPC Flow Logsを保存するS3バケットのオブジェクト保持日数。コスト削減のため、経過後は自動削除する。"
  type        = number
  default     = 30
}
