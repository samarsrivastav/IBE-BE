output "ebs_environment_url" {
  description = "URL for the EB environment"
  value       = module.beanstalk.ebs_environment_url
}

output "api_gateway_invoke_url" {
  description = "Invoke URL for the API Gateway"
  value       = module.api_gateway.api_gateway_invoke_url
}
