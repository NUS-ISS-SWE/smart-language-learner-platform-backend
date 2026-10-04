variable "aws_region" {
  description = "AWS region containing the EKS-managed Application Load Balancer"
  type        = string
}

variable "domain_name" {
  description = "Public Route 53 hosted-zone name and apex frontend hostname"
  type        = string

  validation {
    condition     = length(trimspace(var.domain_name)) > 0 && !endswith(var.domain_name, ".")
    error_message = "domain_name must be a non-empty DNS name without a trailing dot."
  }
}

variable "eks_cluster_name" {
  description = "EKS cluster name used to identify the ingress-managed ALB"
  type        = string
}

variable "ingress_namespace" {
  description = "Namespace of the Kubernetes ingress that owns the ALB"
  type        = string
}

variable "ingress_name" {
  description = "Name of the Kubernetes ingress that owns the ALB"
  type        = string
}

variable "create_www_record" {
  description = "Create a www alias in addition to the apex-domain alias"
  type        = bool
  default     = true
}

variable "waf_rate_limit" {
  description = "Maximum requests allowed per source IP to /api during a five-minute window"
  type        = number
  default     = 1000

  validation {
    condition     = var.waf_rate_limit >= 10 && var.waf_rate_limit == floor(var.waf_rate_limit)
    error_message = "waf_rate_limit must be a whole number greater than or equal to 10."
  }
}
