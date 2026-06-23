output "instance_id" {
  description = "EC2 instance ID of the SLLP deployment host"
  value       = aws_instance.sllp_deploy.id
}

output "public_ip" {
  description = "Elastic IP attached to the deployment host"
  value       = aws_eip.sllp_deploy.public_ip
}

output "connect_via_ssm" {
  description = "How to get a shell on the instance without a key pair"
  value       = "aws ssm start-session --target ${aws_instance.sllp_deploy.id} --region ${var.aws_region}"
}
