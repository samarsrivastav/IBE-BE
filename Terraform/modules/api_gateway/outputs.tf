output "api_gateway_id" {
  description = "API Gateway ID"
  value       = aws_api_gateway_rest_api.this.id
}

output "api_gateway_invoke_url" {
  description = "Invoke URL for this API Gateway stage"
  value       = "${aws_api_gateway_rest_api.this.execution_arn}/${var.environment}"
}
