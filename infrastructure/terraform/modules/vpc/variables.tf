variable "project" { type = string }
variable "env" { type = string }
variable "vpc_cidr" { type = string }
variable "public_subnet_cidrs" { type = list(string) }
variable "private_subnet_cidrs" { type = list(string) }
variable "availability_zones" { type = list(string) }

variable "flow_log_retention_days" {
  description = "VPC Flow LogsのCloudWatch Logs保持日数"
  type        = number
  default     = 30
}
