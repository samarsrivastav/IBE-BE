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
