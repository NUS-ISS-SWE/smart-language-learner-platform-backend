resource "aws_secretsmanager_secret" "postgres" {
  name        = "${var.project_name}/postgres"
  description = "PostgreSQL credentials for SLLP"

  recovery_window_in_days = 0
  tags = {
    Name = "${var.project_name}-postgres-secret"
  }
}

resource "aws_secretsmanager_secret" "openai" {
  name        = "${var.project_name}/openai"
  description = "OpenAI credentials for SLLP"

  recovery_window_in_days = 0
  tags = {
    Name = "${var.project_name}-openai-secret"
  }
}

resource "aws_secretsmanager_secret" "jwt" {
  name        = "${var.project_name}/jwt"
  description = "JWT secret for SLLP"

  recovery_window_in_days = 0
  tags = {
    Name = "${var.project_name}-jwt-secret"
  }
}

resource "aws_secretsmanager_secret" "audit_ingest" {
  name        = "${var.project_name}/audit-ingest"
  description = "Shared HMAC secret for SLLP audit event ingestion"

  recovery_window_in_days = 0
  tags = {
    Name = "${var.project_name}-audit-ingest-secret"
  }
}

resource "aws_secretsmanager_secret" "mail" {
  name        = "${var.project_name}/mail"
  description = "SMTP credentials for SLLP email delivery"

  recovery_window_in_days = 0
  tags = {
    Name = "${var.project_name}-mail-secret"
  }
}

resource "aws_secretsmanager_secret" "stripe" {
  name        = "${var.project_name}/stripe"
  description = "Stripe webhook and Payment Link configuration for SLLP"

  recovery_window_in_days = 0
  tags = {
    Name = "${var.project_name}-stripe-secret"
  }
}
