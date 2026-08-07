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

output "config_sns_topic_arn" {
  description = "AWS Config通知用SNSトピックARN"
  value       = module.config.sns_topic_arn
}

output "waf_web_acl_arn" {
  description = "ALBにアソシエートしたWAF WebACLのARN"
  value       = module.waf.web_acl_arn
}

output "alarms_sns_topic_arn" {
  description = "CloudWatchアラーム通知用SNSトピックARN"
  value       = module.alarms.sns_topic_arn
}

output "ops_dashboard_name" {
  description = "CloudWatchダッシュボード名"
  value       = module.alarms.dashboard_name
}

output "cloudtrail_arn" {
  value = module.security_audit.cloudtrail_arn
}

output "guardduty_detector_id" {
  value = module.security_audit.guardduty_detector_id
}
