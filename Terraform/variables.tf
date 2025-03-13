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
}

variable "public_subnets" {
  type = list(string)
}

# No longer declare or pass the secret variables here
# since you're using data sources from SSM instead.
