locals {
  service_identities = {
    admin_service = {
      workload_name   = "admin-service"
      service_account = "sllp-admin-service"
      policy_name     = "${var.project_name}-admin-service-secrets"
      policy_sid      = "ReadAdminServiceSecrets"
      secret_arns = [
        module.secrets.postgres_secret_arn,
        module.secrets.audit_ingest_secret_arn
      ]
    }

    composition_service = {
      workload_name   = "composition-service"
      service_account = "sllp-composition-service"
      policy_name     = "${var.project_name}-composition-service-secrets"
      policy_sid      = "ReadCompositionServiceSecrets"
      secret_arns = [
        module.secrets.postgres_secret_arn,
        module.secrets.audit_ingest_secret_arn
      ]
    }

    document_service = {
      workload_name   = "document-service"
      service_account = "sllp-document-service"
      policy_name     = "${var.project_name}-document-service-secrets"
      policy_sid      = "ReadDocumentServiceSecrets"
      secret_arns = [
        module.secrets.postgres_secret_arn,
        module.secrets.jwt_secret_arn,
        module.secrets.audit_ingest_secret_arn
      ]
    }

    api_gateway = {
      workload_name   = "api-gateway"
      service_account = "sllp-api-gateway"
      policy_name     = "${var.project_name}-api-gateway-secrets"
      policy_sid      = "ReadApiGatewaySecrets"
      secret_arns = [
        module.secrets.jwt_secret_arn,
        module.secrets.audit_ingest_secret_arn
      ]
    }

    liquibase_service = {
      workload_name   = "liquibase-service"
      service_account = "sllp-liquibase-service"
      policy_name     = "${var.project_name}-liquibase-service-secrets"
      policy_sid      = "ReadLiquibaseServiceSecrets"
      secret_arns     = [module.secrets.postgres_secret_arn]
    }

    postgres = {
      workload_name   = "postgres"
      service_account = "postgres-db"
      policy_name     = "${var.project_name}-postgres-secret"
      policy_sid      = "ReadPostgresSecret"
      secret_arns     = [module.secrets.postgres_secret_arn]
    }

    product_guide_service = {
      workload_name   = "product-guide-service"
      service_account = "sllp-product-guide-service"
      policy_name     = "${var.project_name}-product-guide-service-secrets"
      policy_sid      = "ReadProductGuideServiceSecrets"
      secret_arns = [
        module.secrets.postgres_secret_arn,
        module.secrets.openai_secret_arn,
        module.secrets.audit_ingest_secret_arn
      ]
    }

    subscription_service = {
      workload_name   = "subscription-service"
      service_account = "sllp-subscription-service"
      policy_name     = "${var.project_name}-subscription-service-secrets"
      policy_sid      = "ReadSubscriptionServiceSecrets"
      secret_arns = [
        module.secrets.postgres_secret_arn,
        module.secrets.audit_ingest_secret_arn,
        module.secrets.stripe_secret_arn
      ]
    }

    user_service = {
      workload_name   = "user-service"
      service_account = "sllp-user-service"
      policy_name     = "${var.project_name}-user-service-secrets"
      policy_sid      = "ReadUserServiceSecrets"
      secret_arns = [
        module.secrets.postgres_secret_arn,
        module.secrets.jwt_secret_arn,
        module.secrets.audit_ingest_secret_arn,
        module.secrets.mail_secret_arn
      ]
    }
  }
}

module "service_identity" {
  for_each = local.service_identities

  source = "./modules/service-identity"

  project_name    = var.project_name
  cluster_name    = module.eks.cluster_name
  namespace       = "sllp"
  workload_name   = each.value.workload_name
  service_account = each.value.service_account
  policy_name     = each.value.policy_name
  policy_sid      = each.value.policy_sid
  secret_arns     = each.value.secret_arns
}

data "aws_iam_policy_document" "document_service_s3" {
  statement {
    sid    = "AccessDocumentObjects"
    effect = "Allow"

    actions = [
      "s3:DeleteObject",
      "s3:GetObject",
      "s3:PutObject"
    ]

    resources = ["${module.s3.bucket_arn}/*"]
  }
}

resource "aws_iam_role_policy" "document_service_s3" {
  name   = "${var.project_name}-document-service-s3"
  role   = module.service_identity["document_service"].role_name
  policy = data.aws_iam_policy_document.document_service_s3.json
}
