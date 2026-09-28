variable "project_name" {
  description = "Project name used for resource naming"
  type        = string
}

variable "bucket_name" {
  description = "Fixed name of the document upload bucket"
  type        = string
}

variable "domain_name" {
  description = "Application domain allowed to read presigned document URLs"
  type        = string
}
