output "vpc_id" {
  value = module.vpc.vpc_id
}

output "alb_dns_name" {
  description = "ALB DNS名（ブラウザでアクセスするURL）"
  value       = module.alb.alb_dns_name
}

output "ecr_app_repository_url" {
  description = "アプリのECRリポジトリURL"
  value       = module.ecr.app_repository_url
}

output "ecs_cluster_name" {
  value = module.ecs.cluster_name
}

output "ecs_service_name" {
  value = module.ecs.service_name
}

output "pipeline_name" {
  value = module.codepipeline.pipeline_name
}

output "redis_host" {
  description = "ElastiCache Redisエンドポイント"
  value       = module.ecs.redis_host
}

output "ecr_batch_repository_url" {
  description = "バッチのECRリポジトリURL"
  value       = module.ecr.batch_repository_url
}

output "batch_pipeline_name" {
  value = module.codepipeline.batch_pipeline_name
}
