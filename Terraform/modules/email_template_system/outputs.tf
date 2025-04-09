output "s3_bucket_name" {
  description = "Name of the S3 bucket for email templates"
  value       = aws_s3_bucket.email_templates.id
}

output "s3_bucket_arn" {
  description = "ARN of the S3 bucket for email templates"
  value       = aws_s3_bucket.email_templates.arn
}

output "sns_topic_arn" {
  description = "ARN of the SNS topic for email notifications"
  value       = aws_sns_topic.email_notifications.arn
}

output "lambda_function_arn" {
  description = "ARN of the Lambda function"
  value       = aws_lambda_function.email_processor.arn
} 