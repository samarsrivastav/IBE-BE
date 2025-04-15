output "lambda_function_arn" {
  description = "ARN of the Lambda function"
  value       = aws_lambda_function.email_processor.arn
}

output "lambda_function_name" {
  description = "Name of the Lambda function"
  value       = aws_lambda_function.email_processor.function_name
}

output "s3_bucket_name" {
  description = "Name of the S3 bucket for email templates"
  value       = aws_s3_bucket.email_templates.id
}

output "s3_bucket_arn" {
  description = "ARN of the S3 bucket for email templates"
  value       = aws_s3_bucket.email_templates.arn
} 