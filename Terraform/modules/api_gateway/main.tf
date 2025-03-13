
resource "aws_api_gateway_rest_api" "this" {
  name        = local.api_name
  description = "API Gateway for Spring Boot"

  tags = {
    Name    = local.api_name
    Env     = var.environment
    Team    = var.team_name
    Purpose = "KDU-IBE-25"
  }
}

# Example resource path /v1
resource "aws_api_gateway_resource" "v1" {
  rest_api_id = aws_api_gateway_rest_api.this.id
  parent_id   = aws_api_gateway_rest_api.this.root_resource_id
  path_part   = "v1"
}

# ANY method on /v1
resource "aws_api_gateway_method" "v1_any" {
  rest_api_id   = aws_api_gateway_rest_api.this.id
  resource_id   = aws_api_gateway_resource.v1.id
  http_method   = "ANY"
  authorization = "NONE"
}

# REPLACE THIS BLOCK
resource "aws_api_gateway_integration" "v1_integration" {
  rest_api_id             = aws_api_gateway_rest_api.this.id
  resource_id             = aws_api_gateway_resource.v1.id
  http_method             = aws_api_gateway_method.v1_any.http_method
  integration_http_method = "ANY"
  type                    = "HTTP_PROXY"
  uri                     = "http://${var.beanstalk_endpoint_url}/v1"
}

# ADD THESE NEW BLOCKS
# Proxy resource to handle all paths under /v1
resource "aws_api_gateway_resource" "v1_proxy" {
  rest_api_id = aws_api_gateway_rest_api.this.id
  parent_id   = aws_api_gateway_resource.v1.id
  path_part   = "{proxy+}"
}

resource "aws_api_gateway_method" "v1_proxy_any" {
  rest_api_id   = aws_api_gateway_rest_api.this.id
  resource_id   = aws_api_gateway_resource.v1_proxy.id
  http_method   = "ANY"
  authorization = "NONE"
  
  request_parameters = {
    "method.request.path.proxy" = true
  }
}

resource "aws_api_gateway_integration" "v1_proxy_integration" {
  rest_api_id             = aws_api_gateway_rest_api.this.id
  resource_id             = aws_api_gateway_resource.v1_proxy.id
  http_method             = aws_api_gateway_method.v1_proxy_any.http_method
  integration_http_method = "ANY"
  type                    = "HTTP_PROXY"
  uri                     = "http://${var.beanstalk_endpoint_url}/v1/{proxy}"
  
  request_parameters = {
    "integration.request.path.proxy" = "method.request.path.proxy"
  }
}

# MODIFY THIS BLOCK TO INCLUDE THE NEW RESOURCES
resource "aws_api_gateway_deployment" "this" {
  depends_on  = [
    aws_api_gateway_integration.v1_integration,
    aws_api_gateway_integration.v1_proxy_integration
  ]
  rest_api_id = aws_api_gateway_rest_api.this.id
  stage_name = var.environment
}