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
  api_name = "${var.team_name}-api"
}

variable "create_api_gateway" {
  type = bool
  # true => create the API Gateway in this workspace
  # false => skip creation, reference existing by name
}

variable "api_gateway_name" {
  type = string
  # e.g. "genwin-shared-api"
}
