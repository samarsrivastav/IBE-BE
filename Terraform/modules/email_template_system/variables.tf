variable "environment" {
  description = "Environment name (e.g., dev, qa)"
  type        = string
}

variable "team_name" {
  description = "Name of the team"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID where the resources will be created"
  type        = string
}

variable "private_subnets" {
  description = "List of private subnet IDs"
  type        = list(string)
}

variable "public_subnets" {
  description = "List of public subnet IDs"
  type        = list(string)
} 