output "security_group_id" {
  description = "The ID of the Security Group for the Elastic Beanstalk environment"
  value       = aws_security_group.this.id
}
