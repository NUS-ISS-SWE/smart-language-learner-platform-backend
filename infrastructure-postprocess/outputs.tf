output "load_balancer_dns_name" {
  description = "DNS name of the EKS-managed ALB discovered by Terraform"
  value       = data.aws_lb.ingress.dns_name
}

output "frontend_record_fqdns" {
  description = "Route 53 frontend aliases managed by this Terraform root"
  value = {
    for hostname, record in aws_route53_record.frontend : hostname => record.fqdn
  }
}

output "waf_web_acl_name" {
  description = "Name of the regional WAF Web ACL associated with the ingress ALB"
  value       = aws_wafv2_web_acl.ingress.name
}

output "waf_web_acl_arn" {
  description = "ARN of the regional WAF Web ACL associated with the ingress ALB"
  value       = aws_wafv2_web_acl.ingress.arn
}
