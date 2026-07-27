output "config_recorder_name" {
  value = aws_config_configuration_recorder.this.name
}

output "config_bucket_name" {
  value = aws_s3_bucket.config.bucket
}

output "sns_topic_arn" {
  description = "AWS Config通知用SNSトピックARN。Slack通知を追加する場合はAWS Chatbot等でこのトピックをサブスクライブする"
  value       = aws_sns_topic.config_notifications.arn
}
