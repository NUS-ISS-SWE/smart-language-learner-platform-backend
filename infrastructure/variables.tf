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

variable "domain_name" {
  description = "Root domain name"
  type        = string
}