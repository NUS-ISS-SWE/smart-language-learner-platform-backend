locals {
  log_group_name = "/aws/containerinsights/${var.cluster_name}/application"

  application_log_configuration = <<-EOT
    [INPUT]
      Name                tail
      Tag                 application.*
      Path                /var/log/containers/*_${var.application_namespace}_*.log
      multiline.parser    docker, cri
      DB                  /var/fluent-bit/state/flb_sllp_container.db
      Mem_Buf_Limit       50MB
      Skip_Long_Lines     On
      Refresh_Interval    10
      Rotate_Wait         30
      storage.type        filesystem
      Read_from_Head      $${READ_FROM_HEAD}
    [FILTER]
      Name                aws
      Match               application.*
      az                  false
      ec2_instance_id     false
    [FILTER]
      Name                kubernetes
      Match               application.*
      Kube_URL            https://kubernetes.default.svc:443
      Kube_Tag_Prefix     application.var.log.containers.
      Merge_Log           On
      Merge_Log_Key       log_processed
      K8S-Logging.Parser  On
      K8S-Logging.Exclude Off
      Labels              Off
      Annotations         Off
      Use_Kubelet         On
      Kubelet_Port        10250
      Buffer_Size         0
    [OUTPUT]
      Name                cloudwatch_logs
      Match               application.*
      region              $${AWS_REGION}
      log_group_name      ${aws_cloudwatch_log_group.application.name}
      log_stream_prefix   $${HOST_NAME}-
      auto_create_group   false
      extra_user_agent    container-insights
  EOT
}

data "aws_eks_addon_version" "cloudwatch_observability" {
  addon_name         = "amazon-cloudwatch-observability"
  kubernetes_version = var.cluster_version
  most_recent        = true
}

resource "aws_cloudwatch_log_group" "application" {
  name                        = local.log_group_name
  retention_in_days           = var.log_retention_days
  log_group_class             = "STANDARD"
  deletion_protection_enabled = false
  skip_destroy                = false

  tags = {
    Name = "${var.project_name}-application-logs"
  }
}

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

resource "aws_iam_role" "cloudwatch_logs" {
  name               = "${var.project_name}-cloudwatch-logs-role"
  assume_role_policy = data.aws_iam_policy_document.pod_assume_role.json

  tags = {
    Name = "${var.project_name}-cloudwatch-logs-role"
  }
}

data "aws_iam_policy_document" "cloudwatch_logs" {
  statement {
    sid    = "WriteApplicationLogs"
    effect = "Allow"

    actions = [
      "logs:CreateLogStream",
      "logs:DescribeLogStreams",
      "logs:PutLogEvents"
    ]

    resources = ["${aws_cloudwatch_log_group.application.arn}:*"]
  }
}

resource "aws_iam_role_policy" "cloudwatch_logs" {
  name   = "${var.project_name}-cloudwatch-logs"
  role   = aws_iam_role.cloudwatch_logs.id
  policy = data.aws_iam_policy_document.cloudwatch_logs.json
}

resource "aws_eks_addon" "cloudwatch_observability" {
  cluster_name  = var.cluster_name
  addon_name    = "amazon-cloudwatch-observability"
  addon_version = data.aws_eks_addon_version.cloudwatch_observability.version

  resolve_conflicts_on_create = "OVERWRITE"
  resolve_conflicts_on_update = "OVERWRITE"

  pod_identity_association {
    role_arn        = aws_iam_role.cloudwatch_logs.arn
    service_account = "cloudwatch-agent"
  }

  configuration_values = jsonencode({
    agents = []
    applicationSignals = {
      enabled = false
    }
    containerInsights = {
      enabled = false
    }
    otelContainerInsights = {
      enabled = false
    }
    dcgmExporter = {
      enabled = false
    }
    neuronMonitor = {
      enabled = false
    }
    nodeExporter = {
      enabled = false
    }
    kubeStateMetrics = {
      enabled = false
    }
    manager = {
      applicationSignals = {
        autoMonitor = {
          monitorAllServices = false
        }
      }
    }
    containerLogs = {
      enabled = true
      fluentBit = {
        config = {
          extraFiles = {
            "application-log.conf" = local.application_log_configuration
            "dataplane-log.conf"   = ""
            "host-log.conf"        = ""
          }
        }
      }
    }
  })

  tags = {
    Name = "${var.project_name}-cloudwatch-observability"
  }

  depends_on = [aws_iam_role_policy.cloudwatch_logs]
}

resource "aws_cloudwatch_dashboard" "logs" {
  dashboard_name = "${var.project_name}-logs"

  dashboard_body = jsonencode({
    widgets = [
      {
        type   = "text"
        x      = 0
        y      = 0
        width  = 24
        height = 2
        properties = {
          markdown = "# SLLP application logs\nAll container stdout/stderr from the `${var.application_namespace}` namespace. Logs are retained for ${var.log_retention_days} days."
        }
      },
      {
        type   = "log"
        x      = 0
        y      = 2
        width  = 24
        height = 6
        properties = {
          region = var.aws_region
          title  = "Log events by container"
          view   = "timeSeries"
          query  = "SOURCE '${aws_cloudwatch_log_group.application.name}'\n| stats count(*) as events by bin(5m), kubernetes.container_name"
        }
      },
      {
        type   = "log"
        x      = 0
        y      = 8
        width  = 8
        height = 7
        properties = {
          region = var.aws_region
          title  = "Errors by container"
          view   = "bar"
          query  = "SOURCE '${aws_cloudwatch_log_group.application.name}'\n| filter stream = 'stderr' or log like /ERROR|Error|Exception|FAILED|Failed|failed/\n| stats count(*) as errors by kubernetes.container_name\n| sort errors desc"
        }
      },
      {
        type   = "log"
        x      = 8
        y      = 8
        width  = 16
        height = 7
        properties = {
          region = var.aws_region
          title  = "Recent errors"
          view   = "table"
          query  = "SOURCE '${aws_cloudwatch_log_group.application.name}'\n| fields @timestamp, kubernetes.pod_name as pod, kubernetes.container_name as container, stream, log\n| filter stream = 'stderr' or log like /ERROR|Error|Exception|FAILED|Failed|failed/\n| sort @timestamp desc\n| limit 100"
        }
      }
    ]
  })
}
