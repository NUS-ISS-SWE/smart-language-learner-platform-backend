resource "aws_secretsmanager_secret" "postgres" {
  name        = "${var.project_name}/postgres"
  description = "PostgreSQL credentials for SLLP"

  tags = {
    Name = "${var.project_name}-postgres-secret"
  }
}

resource "aws_secretsmanager_secret" "openai" {
  name        = "${var.project_name}/openai"
  description = "OpenAI credentials for SLLP"

  tags = {
    Name = "${var.project_name}-openai-secret"
  }
}

resource "aws_secretsmanager_secret" "jwt" {
  name        = "${var.project_name}/jwt"
  description = "JWT secret for SLLP"

  tags = {
    Name = "${var.project_name}-jwt-secret"
  }
}