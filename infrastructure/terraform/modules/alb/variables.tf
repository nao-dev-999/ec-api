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

