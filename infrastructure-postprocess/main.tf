data "aws_route53_zone" "main" {
  name         = var.domain_name
  private_zone = false
}

# EKS Auto Mode adds these stable tags to the ALB created for an Ingress.
# Looking it up by cluster and ingress identity avoids hard-coding the generated
# ALB name or DNS address.
data "aws_lb" "ingress" {
  tags = {
    "eks:eks-cluster-name"               = var.eks_cluster_name
    "ingress.eks.amazonaws.com/stack"    = "${var.ingress_namespace}/${var.ingress_name}"
    "ingress.eks.amazonaws.com/resource" = "LoadBalancer"
  }
}

locals {
  frontend_hostnames = toset(concat(
    [var.domain_name],
    var.create_www_record ? ["www.${var.domain_name}"] : []
  ))
}

resource "aws_route53_record" "frontend" {
  for_each = local.frontend_hostnames

  zone_id = data.aws_route53_zone.main.zone_id
  name    = each.value
  type    = "A"

  alias {
    name                   = data.aws_lb.ingress.dns_name
    zone_id                = data.aws_lb.ingress.zone_id
    evaluate_target_health = true
  }
}
