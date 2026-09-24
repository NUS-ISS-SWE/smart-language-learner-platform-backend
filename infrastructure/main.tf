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

module "secrets" {
  source = "./modules/secrets"

  project_name = var.project_name
}

module "acm" {
  source = "./modules/acm"

  project_name = var.project_name
  domain_name  = var.domain_name
}