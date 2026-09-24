output "postgres_secret_arn" {
  description = "ARN of the PostgreSQL secret"
  value       = aws_secretsmanager_secret.postgres.arn
}

output "openai_secret_arn" {
  description = "ARN of the OpenAI secret"
  value       = aws_secretsmanager_secret.openai.arn
}

output "jwt_secret_arn" {
  description = "ARN of the JWT secret"
  value       = aws_secretsmanager_secret.jwt.arn
}