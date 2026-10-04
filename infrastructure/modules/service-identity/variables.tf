variable "project_name" {
  description = "Project name used for resource naming"
  type        = string
}

variable "cluster_name" {
  description = "Name of the EKS cluster"
  type        = string
}

variable "namespace" {
  description = "Kubernetes namespace containing the service account"
  type        = string
}

variable "workload_name" {
  description = "Workload name used for IAM role and Pod Identity resource naming"
  type        = string
}

variable "service_account" {
  description = "Kubernetes service account associated with the IAM role"
  type        = string
}

variable "policy_name" {
  description = "Name of the inline Secrets Manager policy"
  type        = string
}

variable "policy_sid" {
  description = "Statement ID used in the Secrets Manager policy"
  type        = string
}

variable "secret_arns" {
  description = "Secrets Manager ARNs the workload may read"
  type        = list(string)
}
