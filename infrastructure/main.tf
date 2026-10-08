module "network" {
  source = "./modules/network"

  project_name = var.project_name
}

module "iam" {
  source = "./modules/iam"

  project_name = var.project_name
}

module "eks" {
  source = "./modules/eks"

  project_name = var.project_name

  cluster_role_arn   = module.iam.eks_cluster_role_arn
  node_role_arn      = module.iam.eks_node_role_arn
  private_subnet_ids = module.network.private_subnet_ids
}

module "observability" {
  source = "./modules/observability"

  project_name          = var.project_name
  aws_region            = var.aws_region
  cluster_name          = module.eks.cluster_name
  cluster_version       = module.eks.cluster_version
  application_namespace = "sllp"
  log_retention_days    = 30
}

module "secrets" {
  source = "./modules/secrets"

  project_name = var.project_name
}

module "s3" {
  source = "./modules/s3"

  project_name = var.project_name
  bucket_name  = var.document_bucket_name
  domain_name  = var.domain_name
}

# Needs the domain's Route 53 hosted zone in this AWS account; off unless enable_acm = true.
module "acm" {
  count  = var.enable_acm ? 1 : 0
  source = "./modules/acm"

  project_name = var.project_name
  domain_name  = var.domain_name
}

# Keeps existing state (created before the count above) pointing at the same certificate.
moved {
  from = module.acm
  to   = module.acm[0]
}
