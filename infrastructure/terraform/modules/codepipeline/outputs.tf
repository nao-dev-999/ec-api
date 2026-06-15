output "pipeline_name" {
  value = aws_codepipeline.this.name
}

output "artifacts_bucket" {
  value = aws_s3_bucket.artifacts.bucket
}
