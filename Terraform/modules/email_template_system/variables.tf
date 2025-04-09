variable "environment" {
  description = "Environment name (e.g., dev, prod)"
  type        = string
}

variable "team_name" {
  description = "Name of the team"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID where the Lambda function will be deployed"
  type        = string
}

variable "private_subnets" {
  description = "List of private subnet IDs where the Lambda function will be deployed"
  type        = list(string)
}

variable "public_subnets" {
  description = "List of public subnet IDs"
  type        = list(string)
}

variable "smtp_host" {
  description = "SMTP server host"
  type        = string
}

variable "smtp_port" {
  description = "SMTP server port"
  type        = string
}

variable "smtp_secure" {
  description = "Whether to use SSL/TLS for SMTP connection"
  type        = string
  default     = "true"
}

variable "smtp_user" {
  description = "SMTP server username"
  type        = string
}

variable "smtp_password" {
  description = "SMTP server password"
  type        = string
  sensitive   = true
}

variable "smtp_from" {
  description = "Email address to send from"
  type        = string
} 