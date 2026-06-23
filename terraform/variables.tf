variable "aws_region" {
  description = "AWS region to deploy into"
  type        = string
  default     = "ap-southeast-1"
}

variable "project_name" {
  description = "Name prefix used for tagging/naming all resources"
  type        = string
  default     = "sllp"
}

variable "instance_type" {
  description = "EC2 instance type for the SLLP deployment host (5 services + frontend via Docker)"
  type        = string
  default     = "t3.medium"
}

variable "app_ports" {
  description = "Inbound TCP ports the SLLP stack needs exposed (frontend, api-gateway, etc.)"
  type        = list(number)
  default     = [80, 443, 8080]
}

variable "allowed_cidr" {
  description = "CIDR allowed to reach the app ports. Phase 1 default is wide open for demo convenience — tighten before this goes anywhere near production."
  type        = string
  default     = "0.0.0.0/0"
}
