####################################
# Conditionally Create API Gateway
####################################
resource "aws_api_gateway_rest_api" "this" {
  count       = var.create_api_gateway ? 1 : 0
  name        = var.api_gateway_name
  description = "Shared API Gateway for Spring Boot"

  tags = {
    Name    = var.api_gateway_name
    Team    = var.team_name
    Purpose = "KDU-IBE-25"
  }
}

# Create the /{proxy+} resource
resource "aws_api_gateway_resource" "root_proxy" {
  count = var.create_api_gateway ? 1 : 0

  rest_api_id = aws_api_gateway_rest_api.this[0].id
  parent_id   = aws_api_gateway_rest_api.this[0].root_resource_id
  path_part   = "{proxy+}"
}

# Create the ANY method
resource "aws_api_gateway_method" "root_proxy_any" {
  count = var.create_api_gateway ? 1 : 0

  rest_api_id   = aws_api_gateway_rest_api.this[0].id
  resource_id   = aws_api_gateway_resource.root_proxy[0].id
  http_method   = "ANY"
  authorization = "NONE"

  request_parameters = {
    "method.request.path.proxy" = true
  }
}

# Create the HTTP proxy integration
resource "aws_api_gateway_integration" "root_proxy_integration" {
  count = var.create_api_gateway ? 1 : 0

  rest_api_id             = aws_api_gateway_rest_api.this[0].id
  resource_id             = aws_api_gateway_resource.root_proxy[0].id
  http_method             = aws_api_gateway_method.root_proxy_any[0].http_method
  integration_http_method = "ANY"
  type                    = "HTTP_PROXY"
  uri                     = "http://${var.beanstalk_endpoint_url}/{proxy}"
  request_parameters = {
    "integration.request.path.proxy" = "method.request.path.proxy"
  }
}

####################################
# Lookup Existing Gateway Resources
####################################
data "aws_api_gateway_rest_api" "existing" {
  count = var.create_api_gateway ? 0 : 1
  name  = var.api_gateway_name
}

# Get existing proxy resource
data "aws_api_gateway_resource" "existing_proxy" {
  count = var.create_api_gateway ? 0 : 1

  rest_api_id = data.aws_api_gateway_rest_api.existing[0].id
  path        = "/{proxy+}"
}

# Update integration for the stage
resource "aws_api_gateway_integration" "stage_specific_integration" {
  count = var.create_api_gateway ? 0 : 1

  rest_api_id             = data.aws_api_gateway_rest_api.existing[0].id
  resource_id             = data.aws_api_gateway_resource.existing_proxy[0].id
  http_method             = "ANY"
  integration_http_method = "ANY"
  type                    = "HTTP_PROXY"
  uri                     = "http://${var.beanstalk_endpoint_url}/{proxy}"
  request_parameters = {
    "integration.request.path.proxy" = "method.request.path.proxy"
  }
}

####################################
# Create Deployment (Stage)
####################################
resource "aws_api_gateway_deployment" "deployment" {
  rest_api_id = var.create_api_gateway ? aws_api_gateway_rest_api.this[0].id : data.aws_api_gateway_rest_api.existing[0].id

  # Use triggers to force redeployment when the integration changes
  triggers = {
    integration_uri = var.create_api_gateway ? aws_api_gateway_integration.root_proxy_integration[0].uri : aws_api_gateway_integration.stage_specific_integration[0].uri
    redeployment   = uuid() # Force redeployment every time to ensure stage-specific changes are applied
  }

  lifecycle {
    create_before_destroy = true
  }

  depends_on = [
    aws_api_gateway_integration.root_proxy_integration,
    aws_api_gateway_integration.stage_specific_integration
  ]
}

resource "aws_api_gateway_stage" "stage" {
  deployment_id = aws_api_gateway_deployment.deployment.id
  rest_api_id   = var.create_api_gateway ? aws_api_gateway_rest_api.this[0].id : data.aws_api_gateway_rest_api.existing[0].id
  stage_name    = var.environment

  variables = {
    environment = var.environment
  }
}