output "postgres_secret_arn" {
  description = "ARN of the PostgreSQL secret"
  value       = aws_secretsmanager_secret.postgres.arn
}

output "openai_secret_arn" {
  description = "ARN of the OpenAI secret"
  value       = aws_secretsmanager_secret.openai.arn
}

output "lakera_secret_arn" {
  description = "ARN of the Lakera Guard secret"
  value       = aws_secretsmanager_secret.lakera.arn
}

output "jwt_secret_arn" {
  description = "ARN of the JWT secret"
  value       = aws_secretsmanager_secret.jwt.arn
}

output "audit_ingest_secret_arn" {
  description = "ARN of the audit ingestion secret"
  value       = aws_secretsmanager_secret.audit_ingest.arn
}

output "internal_service_secret_arn" {
  description = "ARN of the internal service authentication secret"
  value       = aws_secretsmanager_secret.internal_service.arn
}

output "mail_secret_arn" {
  description = "ARN of the SMTP credentials secret"
  value       = aws_secretsmanager_secret.mail.arn
}

output "stripe_secret_arn" {
  description = "ARN of the Stripe configuration secret"
  value       = aws_secretsmanager_secret.stripe.arn
}

output "agent_tokens_secret_arn" {
  description = "ARN of the A2A agent tokens secret"
  value       = aws_secretsmanager_secret.agent_tokens.arn
}
