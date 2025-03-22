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

variable "beanstalk_endpoint_dev" {
  type    = string
  default = "my-dev-eb.example.com"
}

variable "beanstalk_endpoint_qa" {
  type    = string
  default = "my-qa-eb.example.com"
}