output "ebs_environment_url" {
  description = "The endpoint URL of the Elastic Beanstalk environment"
  value       = aws_elastic_beanstalk_environment.this.endpoint_url
}

output "application_name" {
  description = "The name of the Elastic Beanstalk application"
  value       = aws_elastic_beanstalk_application.this.name
}