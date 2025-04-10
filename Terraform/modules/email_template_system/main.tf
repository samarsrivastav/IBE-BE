# Data Sources for SSM Parameters
data "aws_ssm_parameter" "db_user" {
  name            = "/genwin/db-user"
  with_decryption = true
}

data "aws_ssm_parameter" "db_pass" {
  name            = "/genwin/db-pass"
  with_decryption = true
}

data "aws_ssm_parameter" "db_url" {
  name            = "/genwin/${var.environment}/db_url"
  with_decryption = true
}

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

# Lambda Function
resource "aws_lambda_function" "email_processor" {
  filename         = "${path.module}/lambda/function.zip"
  function_name    = "genwin-email-processor"
  role            = aws_iam_role.lambda_role.arn
  handler         = "index.handler"
  runtime         = "nodejs18.x"
  timeout         = 300
  memory_size     = 256

  environment {
    variables = {
      DB_USER       = data.aws_ssm_parameter.db_user.value
      DB_PASSWORD   = data.aws_ssm_parameter.db_pass.value
      DB_HOST       = "ibe2025-kdu25rdsinstance61f66da9-8harocvoxzt8.c3ysg6m2290x.ap-south-1.rds.amazonaws.com"
      DB_NAME       = "Database_8_dev"
      DB_PORT       = "5432"
      SMTP_HOST     = "smtp.gmail.com"
      SMTP_PORT     = "587"
      SMTP_SECURE   = "true"
      SMTP_USER     = "thoravenger56787@gmail.com"
      SMTP_PASSWORD = "iubyrwzmsrlqzyvr"
      SMTP_FROM     = "thoravenger56787@gmail.com"
    }
  }

  vpc_config {
    subnet_ids         = var.private_subnets
    security_group_ids = [aws_security_group.lambda_sg.id]
  }
}

# Lambda Security Group for email processor
resource "aws_security_group" "lambda_sg" {
  name        = "genwin-email-processor-sg"
  description = "Security group for email processor Lambda"
  vpc_id      = var.vpc_id

  ingress {
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]  # This should be restricted to the RDS security group in production
  }

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
          "s3:GetObject",
          "s3:ListBucket"
        ]
        Resource = [
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
      },
      {
        Effect = "Allow"
        Action = [
          "ssm:GetParameter",
          "ssm:GetParameters"
        ]
        Resource = [
          "arn:aws:ssm:*:*:parameter/genwin/*"
        ]
      }
    ]
  })
}

# S3 Event Notification
resource "aws_s3_bucket_notification" "email_templates" {
  bucket = aws_s3_bucket.email_templates.id

  lambda_function {
    lambda_function_arn = aws_lambda_function.email_processor.arn
    events              = ["s3:ObjectCreated:*"]
    filter_prefix       = "genwin-"
  }

  depends_on = [aws_lambda_permission.allow_s3]
}

# Lambda Permission for S3
resource "aws_lambda_permission" "allow_s3" {
  statement_id  = "AllowExecutionFromS3"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.email_processor.function_name
  principal     = "s3.amazonaws.com"
  source_arn    = aws_s3_bucket.email_templates.arn
} 