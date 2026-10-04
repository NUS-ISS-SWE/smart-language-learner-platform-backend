resource "aws_wafv2_regex_pattern_set" "scanner_user_agents" {
  name        = "sllp-scanner-user-agents"
  description = "User-Agent identifiers for common security scanning tools"
  scope       = "REGIONAL"

  regular_expression {
    regex_string = "(nikto|sqlmap|nmap|nessus|nuclei)"
  }

  tags = {
    Name      = "sllp-scanner-user-agents"
    Project   = "sllp"
    ManagedBy = "terraform"
  }
}

resource "aws_wafv2_web_acl" "ingress" {
  name        = "sllp-alb-waf"
  description = "Web application firewall for the SLLP EKS ingress"
  scope       = "REGIONAL"

  default_action {
    allow {}
  }

  rule {
    name     = "blockScanners"
    priority = 0

    action {
      block {}
    }

    statement {
      regex_pattern_set_reference_statement {
        arn = aws_wafv2_regex_pattern_set.scanner_user_agents.arn

        field_to_match {
          single_header {
            name = "user-agent"
          }
        }

        text_transformation {
          priority = 0
          type     = "LOWERCASE"
        }
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "sllp-block-scanners"
      sampled_requests_enabled   = true
    }
  }

  rule {
    name     = "AWSManagedRulesCommonRuleSet"
    priority = 10

    override_action {
      none {}
    }

    statement {
      managed_rule_group_statement {
        name        = "AWSManagedRulesCommonRuleSet"
        vendor_name = "AWS"

        # The application accepts documents up to 25 MB. The managed rule
        # blocks bodies over 8 KB by default, so count this one condition while
        # retaining the blocking actions of every other rule in the group.
        rule_action_override {
          name = "SizeRestrictions_BODY"

          action_to_use {
            count {}
          }
        }
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "sllp-aws-managed-common-rules"
      sampled_requests_enabled   = true
    }
  }

  rule {
    name     = "rateLimit"
    priority = 20

    action {
      block {}
    }

    statement {
      rate_based_statement {
        aggregate_key_type    = "IP"
        evaluation_window_sec = 300
        limit                 = var.waf_rate_limit

        scope_down_statement {
          or_statement {
            statement {
              byte_match_statement {
                positional_constraint = "EXACTLY"
                search_string         = "/api"

                field_to_match {
                  uri_path {}
                }

                text_transformation {
                  priority = 0
                  type     = "NONE"
                }
              }
            }

            statement {
              byte_match_statement {
                positional_constraint = "STARTS_WITH"
                search_string         = "/api/"

                field_to_match {
                  uri_path {}
                }

                text_transformation {
                  priority = 0
                  type     = "NONE"
                }
              }
            }
          }
        }
      }
    }

    visibility_config {
      cloudwatch_metrics_enabled = true
      metric_name                = "sllp-api-rate-limit"
      sampled_requests_enabled   = true
    }
  }

  visibility_config {
    cloudwatch_metrics_enabled = true
    metric_name                = "sllp-alb-waf"
    sampled_requests_enabled   = true
  }

  tags = {
    Name      = "sllp-alb-waf"
    Project   = "sllp"
    ManagedBy = "terraform"
  }
}

resource "aws_wafv2_web_acl_association" "ingress" {
  resource_arn = data.aws_lb.ingress.arn
  web_acl_arn  = aws_wafv2_web_acl.ingress.arn
}
