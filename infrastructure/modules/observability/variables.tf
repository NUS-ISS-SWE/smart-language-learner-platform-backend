variable "project_name" {
  description = "Project name used for resource naming"
  type        = string
}

variable "aws_region" {
  description = "AWS region containing the EKS cluster and CloudWatch resources"
  type        = string
}

variable "cluster_name" {
  description = "Name of the EKS cluster"
  type        = string
}

variable "cluster_version" {
  description = "Kubernetes version used to select a compatible EKS add-on version"
  type        = string
}

variable "application_namespace" {
  description = "Kubernetes namespace whose container logs are collected"
  type        = string
  default     = "sllp"
}

variable "log_retention_days" {
  description = "Number of days that application logs are retained"
  type        = number
  default     = 30

  validation {
    condition     = contains([1, 3, 5, 7, 14, 30, 60, 90, 120, 150, 180, 365, 400, 545, 731, 1096, 1827, 2192, 2557, 2922, 3288, 3653], var.log_retention_days)
    error_message = "log_retention_days must be a retention period supported by CloudWatch Logs."
  }
}
