output "role_arn" {
  description = "IAM role ARN used by the workload through EKS Pod Identity"
  value       = aws_iam_role.this.arn
}

output "role_name" {
  description = "IAM role name used when attaching workload-specific policies"
  value       = aws_iam_role.this.name
}
