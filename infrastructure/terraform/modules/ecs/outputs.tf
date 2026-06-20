output "cluster_name" {
  value = aws_ecs_cluster.this.name
}

output "cluster_arn" {
  value = aws_ecs_cluster.this.arn
}

output "service_name" {
  value = aws_ecs_service.app.name
}

output "flyway_task_definition_arn" {
  value = aws_ecs_task_definition.flyway.arn
}

output "flyway_task_definition_family" {
  value = aws_ecs_task_definition.flyway.family
}

output "ecs_sg_id" {
  value = aws_security_group.ecs.id
}

output "task_execution_role_arn" {
  value = aws_iam_role.task_execution.arn
}

output "task_role_arn" {
  value = aws_iam_role.task.arn
}

output "redis_host" {
  value = aws_elasticache_cluster.this.cache_nodes[0].address
}

output "jwt_secret_arn" {
  description = "The ARN of the automatically generated JWT secret SSM parameter"
  value       = aws_ssm_parameter.jwt_secret.arn
}
