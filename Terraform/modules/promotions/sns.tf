resource "aws_sns_topic" "promotional_emails" {
  name = "${var.team_name}-${var.environment}-promotional-emails"
}

resource "aws_sns_topic_subscription" "email_subscription" {
  count     = length(var.subscriber_emails)
  topic_arn = aws_sns_topic.promotional_emails.arn
  protocol  = "email"
  endpoint  = var.subscriber_emails[count.index]
}

# Output the SNS topic ARN for use in the Lambda function
output "sns_topic_arn" {
  value = aws_sns_topic.promotional_emails.arn
} 