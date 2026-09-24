data "aws_route53_zone" "main" {
  name         = var.domain_name
  private_zone = false
}

resource "aws_acm_certificate" "main" {
  domain_name = var.domain_name

  subject_alternative_names = [
    "*.${var.domain_name}"
  ]

  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }

  tags = {
    Name = "${var.project_name}-certificate"
  }
}

resource "aws_route53_record" "validation" {
  zone_id = data.aws_route53_zone.main.zone_id

  name = tolist(
    aws_acm_certificate.main.domain_validation_options
  )[0].resource_record_name

  type = tolist(
    aws_acm_certificate.main.domain_validation_options
  )[0].resource_record_type

  records = [
    tolist(
      aws_acm_certificate.main.domain_validation_options
    )[0].resource_record_value
  ]

  ttl = 60
}

resource "aws_acm_certificate_validation" "main" {
  certificate_arn = aws_acm_certificate.main.arn

  validation_record_fqdns = [
    aws_route53_record.validation.fqdn
  ]
}