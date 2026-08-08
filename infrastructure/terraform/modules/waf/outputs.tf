output "web_acl_arn" {
  value = aws_wafv2_web_acl.this.arn
}

output "web_acl_id" {
  value = aws_wafv2_web_acl.this.id
}

output "waf_log_bucket_name" {
  value       = aws_s3_bucket.waf_logs.id
  description = "WAFログを保存するS3バケット名（将来Athenaでの分析等に使用）"
}

output "waf_log_bucket_arn" {
  value       = aws_s3_bucket.waf_logs.arn
  description = "WAFログを保存するS3バケットのARN"
}
