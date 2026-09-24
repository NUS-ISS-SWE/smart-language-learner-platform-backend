output "eks_cluster_name" {
  description = "EKS cluster name"
  value       = module.eks.cluster_name
}

output "acm_certificate_arn" {
  description = "ACM certificate ARN"
  value       = module.acm.certificate_arn
}

output "route53_hosted_zone_id" {
  description = "Route53 hosted zone ID"
  value       = module.acm.hosted_zone_id
}