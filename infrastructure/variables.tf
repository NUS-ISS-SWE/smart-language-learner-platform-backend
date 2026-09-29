variable "aws_region" {
  description = "AWS region for SLLP infrastructure"
  type        = string
  default     = "ap-southeast-1"
}

variable "project_name" {
  description = "Project name"
  type        = string
  default     = "sllp"
}

variable "document_bucket_name" {
  description = "Fixed S3 bucket name used by the document service"
  type        = string
}

variable "domain_name" {
  description = "Root domain name"
  type        = string
}

variable "enable_acm" {
  description = "Create the ACM certificate (requires the domain's Route 53 hosted zone in this AWS account)"
  type        = bool
  default     = false
}
