################################################
# Terraform Backend Configuration
################################################
terraform {
  backend "s3" {
    bucket         = "genwin-state-bucket"
    key            = "genwin/backend/env-dev/terraform.tfstate"
    region         = "ap-south-1"
    encrypt        = true
    dynamodb_table = "genwin-state-table"
  }
}

################################################
# Data Sources (SSM Parameters)
################################################
data "aws_ssm_parameter" "db_user" {
  name            = "/genwin/db-user"
  with_decryption = true
}

data "aws_ssm_parameter" "db_pass" {
  name            = "/genwin/db-pass"
  with_decryption = true
}

data "aws_ssm_parameter" "db_url" {
  # Picks dev or qa from var.environment
  name            = "/genwin/${var.environment}/db_url"
  with_decryption = true
}

data "aws_ssm_parameter" "jwt_secret" {
  name            = "/genwin/jwt-secret"
  with_decryption = true
}

################################################
# Security Group Module
################################################
module "security_group" {
  source      = "./modules/security_group"
  team_name   = var.team_name
  environment = var.environment
  vpc_id      = var.vpc_id
}

################################################
# Elastic Beanstalk Module
################################################
module "beanstalk" {
  source             = "./modules/beanstalk"
  team_name          = var.team_name
  environment        = var.environment
  vpc_id             = var.vpc_id
  private_subnets    = var.private_subnets
  public_subnets     = var.public_subnets

  # Use the SG ID from our new security_group module
  security_group_ids = [module.security_group.security_group_id]

  # Pass the SSM values to your module
  jwt_secret         = data.aws_ssm_parameter.jwt_secret.value
  postgres_url       = data.aws_ssm_parameter.db_url.value
  postgres_user      = data.aws_ssm_parameter.db_user.value
  postgres_password  = data.aws_ssm_parameter.db_pass.value
}

################################################
# Conditionally Create API Gateway (dev only)
################################################
locals {
  create_api_gateway = (var.environment == "dev")
}

module "api_gateway" {
  source                 = "./modules/api_gateway"
  team_name              = var.team_name
  environment            = var.environment
  create_api_gateway     = local.create_api_gateway
  api_gateway_name       = "genwin-shared-api"

  # We'll use the actual EB URL from the Beanstalk module output
  beanstalk_endpoint_url = module.beanstalk.ebs_environment_url
}
