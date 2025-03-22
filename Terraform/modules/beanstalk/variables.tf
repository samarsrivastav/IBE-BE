variable "team_name" {
  type    = string
  default = "genwin"
}

variable "environment" {
  type    = string
  default = "dev"
}

variable "vpc_id" {
  type = string
}

variable "private_subnets" {
  type = list(string)
  description = "List of private subnet IDs where EB instances will run"
}

variable "public_subnets" {
  type = list(string)
  description = "List of public subnet IDs for the load balancer"
}

variable "security_group_ids" {
  type = list(string)
  description = "List of security group IDs for the EB environment"
}

variable "solution_stack_name" {
  type    = string
  default = "64bit Amazon Linux 2 v3.7.12 running Corretto 17"
}

variable "jwt_secret" {
  type = string
}

variable "postgres_url" {
  type = string
}

variable "postgres_user" {
  type = string
}

variable "postgres_password" {
  type = string
}



