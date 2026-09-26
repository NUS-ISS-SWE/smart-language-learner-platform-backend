output "role_arn" {
  description = "IAM role ARN used by the workload through EKS Pod Identity"
  value       = aws_iam_role.this.arn
}
