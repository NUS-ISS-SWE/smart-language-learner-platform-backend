variable "project_name" {
  description = "Project name used for resource naming"
  type        = string
}

variable "aws_region" {
  description = "AWS region containing the bucket"
  type        = string
}

variable "domain_name" {
  description = "Application domain allowed to read presigned document URLs"
  type        = string
}
