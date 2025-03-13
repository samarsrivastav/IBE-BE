variable "team_name" {
  type = string
}

variable "environment" {
  type = string
}

variable "beanstalk_endpoint_url" {
  type = string
}

locals {
  # Example: genwin-api-dev
  api_name = "${var.team_name}-api-${var.environment}"
}
