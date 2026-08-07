output "sns_topic_arn" {
  description = "アラーム通知用SNSトピックARN"
  value       = aws_sns_topic.alarms.arn
}

output "dashboard_name" {
  description = "CloudWatchダッシュボード名"
  value       = aws_cloudwatch_dashboard.ops.dashboard_name
}
