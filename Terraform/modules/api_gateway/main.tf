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
# Lookup Existing Gateway by Name
# (For workspaces that skip creation)
####################################
data "aws_api_gateway_rest_api" "existing" {
  count = var.create_api_gateway ? 0 : 1
  name  = var.api_gateway_name
}

####################################
# Create Deployment (Stage)
# Always create a stage in each workspace, referencing the correct API.
####################################
resource "aws_api_gateway_deployment" "stage_deployment" {
  rest_api_id = var.create_api_gateway?aws_api_gateway_rest_api.this[0].id: data.aws_api_gateway_rest_api.existing[0].id

  stage_name = var.environment

  # Use triggers to force redeployment when the integration changes.
  triggers = var.create_api_gateway ? {
    integration_uri = aws_api_gateway_integration.root_proxy_integration[0].uri
  } : {}
}