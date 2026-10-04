data "aws_iam_policy_document" "pod_assume_role" {
  statement {
    effect = "Allow"

    principals {
      type        = "Service"
      identifiers = ["pods.eks.amazonaws.com"]
    }

    actions = [
      "sts:AssumeRole",
      "sts:TagSession"
    ]
  }
}

resource "aws_iam_role" "this" {
  name               = "${var.project_name}-${var.workload_name}-role"
  assume_role_policy = data.aws_iam_policy_document.pod_assume_role.json

  tags = {
    Name = "${var.project_name}-${var.workload_name}-role"
  }
}

data "aws_iam_policy_document" "secrets" {
  statement {
    sid    = var.policy_sid
    effect = "Allow"

    actions = [
      "secretsmanager:DescribeSecret",
      "secretsmanager:GetSecretValue"
    ]

    resources = var.secret_arns
  }
}

resource "aws_iam_role_policy" "secrets" {
  name   = var.policy_name
  role   = aws_iam_role.this.id
  policy = data.aws_iam_policy_document.secrets.json
}

resource "aws_eks_pod_identity_association" "this" {
  cluster_name    = var.cluster_name
  namespace       = var.namespace
  service_account = var.service_account
  role_arn        = aws_iam_role.this.arn

  tags = {
    Name = "${var.project_name}-${var.workload_name}-pod-identity"
  }
}
