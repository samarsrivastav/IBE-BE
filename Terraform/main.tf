

provider "aws" {
  region = "ap-south-1"
}
terraform {
  backend "s3" {
    bucket         = "genwin-state-bucket"
    key            = "genwin/backend/terraform.tfstate"
    region         = "ap-south-1"
    encrypt        = true
    dynamodb_table = "genwin-state-table"
  }
}


data "aws_ssm_parameter" "db_user" {
  name            = "/genwin/db-user"
  with_decryption = true
}

data "aws_ssm_parameter" "db_pass" {
  name            = "/genwin/db-pass"
  with_decryption = true
}

data "aws_ssm_parameter" "db_url" {
  # This uses the environment variable to pick dev or qa
  name            = "/genwin/${var.environment}/db_url"
  with_decryption = true
}

data "aws_ssm_parameter" "jwt_secret" {
  name            = "/genwin/jwt-secret"
  with_decryption = true
}

############################################
# Security Group for Elastic Beanstalk Instances (Optional Example)
############################################
resource "aws_security_group" "beanstalk_sg" {
  name        = "${var.team_name}-beanstalk-sg-${var.environment}"
  description = "Security group for EB instances"
  vpc_id      = var.vpc_id

  ingress {
    description = "Allow traffic on port 8080"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]  # Restrict in production
  }

  egress {
    description = "Allow all outbound"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.team_name}-beanstalk-sg-${var.environment}"
  }
}

############################################
# Elastic Beanstalk Module
############################################
module "beanstalk" {
  source             = "./modules/beanstalk"
  team_name          = var.team_name
  environment        = var.environment
  vpc_id             = var.vpc_id
  private_subnets    = var.private_subnets
  public_subnets     = var.public_subnets
  security_group_ids = [aws_security_group.beanstalk_sg.id]

  # Pass the SSM values to your module
  jwt_secret         = data.aws_ssm_parameter.jwt_secret.value
  postgres_url       = data.aws_ssm_parameter.db_url.value
  postgres_user      = data.aws_ssm_parameter.db_user.value
  postgres_password  = data.aws_ssm_parameter.db_pass.value
}

############################################
# API Gateway Module (if applicable)
############################################
module "api_gateway" {
  source                 = "./modules/api_gateway"
  team_name              = var.team_name
  environment            = var.environment
  beanstalk_endpoint_url = module.beanstalk.ebs_environment_url
}
