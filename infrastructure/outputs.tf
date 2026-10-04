output "eks_cluster_name" {
  description = "EKS cluster name"
  value       = module.eks.cluster_name
}

output "cloudwatch_application_log_group_name" {
  description = "CloudWatch log group containing logs from workloads in the sllp namespace"
  value       = module.observability.application_log_group_name
}

output "cloudwatch_logs_dashboard_name" {
  description = "CloudWatch dashboard for SLLP application logs"
  value       = module.observability.dashboard_name
}

output "acm_certificate_arn" {
  description = "ACM certificate ARN"
  value       = module.acm.certificate_arn
}

output "route53_hosted_zone_id" {
  description = "Route53 hosted zone ID"
  value       = module.acm.hosted_zone_id
}

output "admin_service_role_arn" {
  description = "IAM role used by the admin service through EKS Pod Identity"
  value       = module.service_identity["admin_service"].role_arn
}

output "composition_service_role_arn" {
  description = "IAM role used by the composition service through EKS Pod Identity"
  value       = module.service_identity["composition_service"].role_arn
}

output "document_service_role_arn" {
  description = "IAM role used by the document service through EKS Pod Identity"
  value       = module.service_identity["document_service"].role_arn
}

output "evidence_service_role_arn" {
  description = "IAM role used by the evidence service through EKS Pod Identity"
  value       = module.service_identity["evidence_service"].role_arn
}

output "evaluation_service_role_arn" {
  description = "IAM role used by the evaluation service through EKS Pod Identity"
  value       = module.service_identity["evaluation_service"].role_arn
}

output "review_service_role_arn" {
  description = "IAM role used by the review service through EKS Pod Identity"
  value       = module.service_identity["review_service"].role_arn
}

output "suggestions_agent_role_arn" {
  description = "IAM role used by the suggestions agent through EKS Pod Identity"
  value       = module.service_identity["suggestions_agent"].role_arn
}

output "orchestrator_service_role_arn" {
  description = "IAM role used by the orchestrator service through EKS Pod Identity"
  value       = module.service_identity["orchestrator_service"].role_arn
}

output "product_guide_service_role_arn" {
  description = "IAM role used by the product guide service through EKS Pod Identity"
  value       = module.service_identity["product_guide_service"].role_arn
}

output "api_gateway_role_arn" {
  description = "IAM role used by the API gateway through EKS Pod Identity"
  value       = module.service_identity["api_gateway"].role_arn
}

output "postgres_secret_arn" {
  description = "ARN of the PostgreSQL credentials secret"
  value       = module.secrets.postgres_secret_arn
}

output "audit_ingest_secret_arn" {
  description = "ARN of the audit ingestion secret"
  value       = module.secrets.audit_ingest_secret_arn
}

output "user_service_role_arn" {
  description = "IAM role used by the user service through EKS Pod Identity"
  value       = module.service_identity["user_service"].role_arn
}

output "mail_secret_arn" {
  description = "ARN of the SMTP credentials secret"
  value       = module.secrets.mail_secret_arn
}

output "stripe_secret_arn" {
  description = "ARN of the Stripe configuration secret"
  value       = module.secrets.stripe_secret_arn
}

output "agent_tokens_secret_arn" {
  description = "ARN of the A2A agent tokens secret"
  value       = module.secrets.agent_tokens_secret_arn
}

output "subscription_service_role_arn" {
  description = "IAM role used by the subscription service through EKS Pod Identity"
  value       = module.service_identity["subscription_service"].role_arn
}

output "liquibase_service_role_arn" {
  description = "IAM role used by Liquibase through EKS Pod Identity"
  value       = module.service_identity["liquibase_service"].role_arn
}

output "postgres_role_arn" {
  description = "IAM role used by PostgreSQL through EKS Pod Identity"
  value       = module.service_identity["postgres"].role_arn
}

output "document_bucket_name" {
  description = "Name of the S3 bucket used by the document service"
  value       = module.s3.bucket_name
}

output "document_bucket_arn" {
  description = "ARN of the S3 bucket used by the document service"
  value       = module.s3.bucket_arn
}
