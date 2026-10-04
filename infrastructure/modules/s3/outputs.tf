output "bucket_name" {
  description = "Name of the document upload bucket"
  value       = aws_s3_bucket.documents.id
}

output "bucket_arn" {
  description = "ARN of the document upload bucket"
  value       = aws_s3_bucket.documents.arn
}

output "bucket_regional_domain_name" {
  description = "Regional domain name of the document upload bucket"
  value       = aws_s3_bucket.documents.bucket_regional_domain_name
}
