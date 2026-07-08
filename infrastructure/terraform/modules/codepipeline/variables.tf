variable "project" { type = string }
variable "env" { type = string }
variable "aws_region" { type = string }

variable "github_repository" {
  description = "GitHub repository (owner/repo)"
  type        = string
}

variable "github_branch" {
  description = "Branch to trigger pipeline"
  type        = string
  default     = "main"
}

variable "app_repository_url" {
  description = "ECR repository URL for the app image"
  type        = string
}

variable "flyway_repository_url" {
  description = "ECR repository URL for the flyway migration image"
  type        = string
}

variable "ecs_cluster_name" {
  description = "ECS cluster name"
  type        = string
}

variable "ecs_service_name" {
  description = "ECS service name"
  type        = string
}

variable "flyway_task_definition_family" {
  description = "Flyway ECS task definition family name"
  type        = string
}

variable "flyway_subnet_id" {
  description = "Subnet ID for Flyway ECS task"
  type        = string
}

variable "flyway_sg_id" {
  description = "Security Group ID for Flyway ECS task"
  type        = string
}

variable "task_execution_role_arn" {
  type = string
}

variable "task_role_arn" {
  type = string
}