# --- Network: reuse the account's default VPC for phase1 ---
data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

data "aws_ssm_parameter" "al2023_ami" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}

# --- Security group ---
resource "aws_security_group" "sllp_deploy" {
  name        = "${var.project_name}-deploy-sg"
  description = "Inbound app traffic for the SLLP deployment host; SSH intentionally not opened"
  vpc_id      = data.aws_vpc.default.id

  dynamic "ingress" {
    for_each = var.app_ports
    content {
      description = "App port ${ingress.value}"
      from_port   = ingress.value
      to_port     = ingress.value
      protocol    = "tcp"
      cidr_blocks = [var.allowed_cidr]
    }
  }

  egress {
    description = "Allow all outbound (SSM, Docker Hub pulls, vulnerability DB updates, etc.)"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name    = "${var.project_name}-deploy-sg"
    Project = var.project_name
    Phase   = "1"
  }
}

# --- IAM role/profile so the instance is reachable via SSM Session Manager ---
resource "aws_iam_role" "ssm_role" {
  name = "${var.project_name}-ec2-ssm-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
    }]
  })

  tags = {
    Project = var.project_name
    Phase   = "1"
  }
}

resource "aws_iam_role_policy_attachment" "ssm_core" {
  role       = aws_iam_role.ssm_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "ssm_profile" {
  name = "${var.project_name}-ec2-ssm-profile"
  role = aws_iam_role.ssm_role.name
}

# --- EC2 instance: SLLP deployment target ---
resource "aws_instance" "sllp_deploy" {
  ami                    = data.aws_ssm_parameter.al2023_ami.value
  instance_type          = var.instance_type
  subnet_id              = data.aws_subnets.default.ids[0]
  vpc_security_group_ids = [aws_security_group.sllp_deploy.id]
  iam_instance_profile   = aws_iam_instance_profile.ssm_profile.name

  user_data = <<-EOF
              #!/bin/bash
              dnf update -y
              dnf install -y docker
              systemctl enable --now docker
              usermod -aG docker ec2-user
              curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 \
                -o /usr/local/bin/docker-compose
              chmod +x /usr/local/bin/docker-compose
              EOF

  tags = {
    Name    = "${var.project_name}-deploy"
    Project = var.project_name
    Phase   = "1"
  }
}

# --- Elastic IP, managed declaratively ---
resource "aws_eip" "sllp_deploy" {
  domain = "vpc"

  tags = {
    Name    = "${var.project_name}-deploy-eip"
    Project = var.project_name
  }
}

resource "aws_eip_association" "sllp_deploy" {
  instance_id   = aws_instance.sllp_deploy.id
  allocation_id = aws_eip.sllp_deploy.id
}
