output "cloudtrail_arn" {
  value = aws_cloudtrail.this.arn
}

output "cloudtrail_bucket_name" {
  value = aws_s3_bucket.cloudtrail.bucket
}

output "guardduty_detector_id" {
  value = aws_guardduty_detector.this.id
}
