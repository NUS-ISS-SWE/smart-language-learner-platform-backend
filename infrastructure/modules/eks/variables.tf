variable "project_name" {
  description = "Project name used for resource naming"
  type        = string
}

variable "cluster_role_arn" {
  description = "IAM role ARN used by the EKS cluster"
  type        = string
}

variable "node_role_arn" {
  description = "IAM role ARN used by EKS Auto Mode nodes"
  type        = string
}

variable "private_subnet_ids" {
  description = "Private subnet IDs used by EKS"
  type        = list(string)
}