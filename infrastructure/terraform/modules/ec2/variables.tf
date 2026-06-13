variable "project" { type = string }
variable "env" { type = string }
variable "subnet_id" { type = string }
variable "security_group_ids" { type = list(string) }
variable "ami_id" { type = string }
variable "instance_type" { type = string }
