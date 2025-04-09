resource "aws_lambda_function" "promotional_email_sender" {
  filename         = data.archive_file.lambda_zip.output_path
  function_name    = "${var.team_name}-${var.environment}-promotional-email-sender"
  role            = aws_iam_role.lambda_role.arn
  handler         = "index.handler"
  runtime         = "nodejs18.x"
  timeout         = 300
  memory_size     = 256

  environment {
    variables = {
      POSTGRES_URL = var.postgres_url
      SNS_TOPIC_ARN = aws_sns_topic.promotional_emails.arn
    }
  }

  vpc_config {
    subnet_ids         = var.private_subnets
    security_group_ids = [aws_security_group.lambda_sg.id]
  }
}

resource "aws_security_group" "lambda_sg" {
  name        = "${var.team_name}-${var.environment}-lambda-sg"
  description = "Security group for promotional email Lambda function"
  vpc_id      = var.vpc_id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_iam_role" "lambda_role" {
  name = "${var.team_name}-${var.environment}-promotional-email-lambda-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy" "lambda_policy" {
  name = "${var.team_name}-${var.environment}-promotional-email-lambda-policy"
  role = aws_iam_role.lambda_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "sns:Publish"
        ]
        Resource = [
          aws_s3_bucket.email_templates.arn,
          "${aws_s3_bucket.email_templates.arn}/*",
          aws_sns_topic.promotional_emails.arn
        ]
      },
      {
        Effect = "Allow"
        Action = [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents"
        ]
        Resource = "arn:aws:logs:*:*:*"
      }
    ]
  })
}

# Create a zip file for the Lambda function
data "archive_file" "lambda_zip" {
  type        = "zip"
  output_path = "${path.module}/lambda/promotional-email-sender.zip"
  source_dir  = "${path.module}/lambda/src"
} 