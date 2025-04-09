variable "team_name" {
  description = "Name of the team/project"
  type        = string
}

variable "environment" {
  description = "Environment name (e.g., dev, qa, prod)"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID where the Lambda function will be deployed"
  type        = string
}

variable "private_subnets" {
  description = "List of private subnet IDs"
  type        = list(string)
}

variable "postgres_url" {
  description = "PostgreSQL database URL"
  type        = string
  sensitive   = true
}

variable "subscriber_emails" {
  description = "List of email addresses to subscribe to the SNS topic"
  type        = list(string)
  default     = []
} 