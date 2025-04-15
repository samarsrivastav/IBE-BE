output "api_gateway_id" {
  description = "The ID of the shared API Gateway"
  value = var.create_api_gateway ? aws_api_gateway_rest_api.this[0].id : data.aws_api_gateway_rest_api.existing[0].id
}

output "api_gateway_invoke_url" {
  description = "Invoke URL for this API Gateway stage"
  value = var.create_api_gateway ? "${aws_api_gateway_rest_api.this[0].execution_arn}/${var.environment}": "${data.aws_api_gateway_rest_api.existing[0].execution_arn}/${var.environment}"
}
