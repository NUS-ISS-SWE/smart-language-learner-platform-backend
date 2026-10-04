output "application_log_group_name" {
  description = "CloudWatch log group containing application logs from the configured namespace"
  value       = aws_cloudwatch_log_group.application.name
}

output "dashboard_name" {
  description = "Name of the CloudWatch application logs dashboard"
  value       = aws_cloudwatch_dashboard.logs.dashboard_name
}

output "collector_role_arn" {
  description = "IAM role assumed by the CloudWatch logging add-on through EKS Pod Identity"
  value       = aws_iam_role.cloudwatch_logs.arn
}
