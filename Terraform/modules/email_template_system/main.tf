# S3 Bucket for email templates
resource "aws_s3_bucket" "email_templates" {
  bucket = "genwin-dev-email-templates"
}

resource "aws_s3_bucket_versioning" "email_templates" {
  bucket = aws_s3_bucket.email_templates.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "email_templates" {
  bucket = aws_s3_bucket.email_templates.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# SNS Topic for email notifications
resource "aws_sns_topic" "email_notifications" {
  name = "genwin-email-notifications"
}

# SNS Topic Policy
resource "aws_sns_topic_policy" "email_notifications" {
  arn = aws_sns_topic.email_notifications.arn

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "s3.amazonaws.com"
        }
        Action   = "sns:Publish"
        Resource = aws_sns_topic.email_notifications.arn
      }
    ]
  })
}

# Lambda Function
resource "aws_lambda_function" "email_processor" {
  filename         = data.archive_file.lambda_zip.output_path
  function_name    = "genwin-email-processor"
  role            = aws_iam_role.lambda_role.arn
  handler         = "index.handler"
  runtime         = "nodejs18.x"
  timeout         = 300
  memory_size     = 256

  environment {
    variables = {
      SNS_TOPIC_ARN = aws_sns_topic.email_notifications.arn
      SENDER_EMAIL  = "genwin.kdu@yopmail.com"
      DB_USER       = data.aws_ssm_parameter.db_user.value
      DB_PASSWORD   = data.aws_ssm_parameter.db_pass.value
      DB_HOST       = data.aws_ssm_parameter.db_url.value
      DB_NAME       = "postgres"
    }
  }

  vpc_config {
    subnet_ids         = var.private_subnets
    security_group_ids = [aws_security_group.lambda_sg.id]
  }
}

# Lambda Security Group
resource "aws_security_group" "lambda_sg" {
  name        = "genwin-email-processor-sg"
  description = "Security group for email processor Lambda"
  vpc_id      = var.vpc_id

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# Lambda IAM Role
resource "aws_iam_role" "lambda_role" {
  name = "genwin-email-processor-role"

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

# Lambda IAM Policy
resource "aws_iam_role_policy" "lambda_policy" {
  name = "genwin-email-processor-policy"
  role = aws_iam_role.lambda_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "sns:Publish",
          "s3:GetObject",
          "s3:ListBucket"
        ]
        Resource = [
          aws_sns_topic.email_notifications.arn,
          "${aws_s3_bucket.email_templates.arn}/*",
          aws_s3_bucket.email_templates.arn
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
      },
      {
        Effect = "Allow"
        Action = [
          "ec2:CreateNetworkInterface",
          "ec2:DescribeNetworkInterfaces",
          "ec2:DeleteNetworkInterface",
          "ec2:AssignPrivateIpAddresses",
          "ec2:UnassignPrivateIpAddresses"
        ]
        Resource = "*"
      }
    ]
  })
}

# S3 Event Notification
resource "aws_s3_bucket_notification" "email_templates" {
  bucket = aws_s3_bucket.email_templates.id

  topic {
    topic_arn     = aws_sns_topic.email_notifications.arn
    events        = ["s3:ObjectCreated:*"]
    filter_prefix = "genwin-"
  }
}

# Lambda Permission for SNS
resource "aws_lambda_permission" "with_sns" {
  statement_id  = "AllowExecutionFromSNS"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.email_processor.function_name
  principal     = "sns.amazonaws.com"
  source_arn    = aws_sns_topic.email_notifications.arn
}

# SNS Topic Subscription to Lambda
resource "aws_sns_topic_subscription" "lambda_target" {
  topic_arn = aws_sns_topic.email_notifications.arn
  protocol  = "lambda"
  endpoint  = aws_lambda_function.email_processor.arn
}

# Lambda Function Code
data "archive_file" "lambda_zip" {
  type        = "zip"
  output_path = "${path.module}/lambda.zip"

  source {
    content  = file("${path.module}/lambda/index.js")
    filename = "index.js"
  }

  source {
    content  = file("${path.module}/lambda/package.json")
    filename = "package.json"
  }
} 