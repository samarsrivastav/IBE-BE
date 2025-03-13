locals {
  application_name = "${var.team_name}-backend-app"
  environment_name = "${var.team_name}-backend-${var.environment}"
}

############################################
# IAM Role, Policy Attachment, and Instance Profile
############################################

resource "aws_iam_role" "beanstalk_instance_role" {
  name = "${var.team_name}-beanstalk-instance-${var.environment}"
  assume_role_policy = jsonencode({
    Version = "2012-10-17",
    Statement = [{
      Effect = "Allow",
      Principal = {
        Service = "ec2.amazonaws.com"
      },
      Action = "sts:AssumeRole"
    }]
  })

  tags = {
    Name = "${var.team_name}-beanstalk-instance-${var.environment}"
  }
}

resource "aws_iam_role_policy_attachment" "beanstalk_instance_role_attachment" {
  role       = aws_iam_role.beanstalk_instance_role.name
  policy_arn = "arn:aws:iam::aws:policy/AWSElasticBeanstalkWebTier"
}

resource "aws_iam_instance_profile" "beanstalk_instance_profile" {
  name = "${var.team_name}-beanstalk-instance-profile-${var.environment}"
  role = aws_iam_role.beanstalk_instance_role.name
}

############################################
# Elastic Beanstalk Application
############################################

resource "aws_elastic_beanstalk_application" "this" {
  name        = local.application_name
  description = "Elastic Beanstalk Application for Spring Boot"

  tags = {
    Name    = local.application_name
    Env     = var.environment
    Team    = var.team_name
    Purpose = "SpringBootApp"
  }
}

############################################
# Elastic Beanstalk Environment
############################################

resource "aws_elastic_beanstalk_environment" "this" {
  name                = local.environment_name
  application         = aws_elastic_beanstalk_application.this.name
  solution_stack_name = var.solution_stack_name

  # Application environment variables
  setting {
    namespace = "aws:elasticbeanstalk:application:environment"
    name      = "JWT_SECRET"
    value     = var.jwt_secret
  }
  setting {
    namespace = "aws:elasticbeanstalk:application:environment"
    name      = "POSTGRES_URL"
    value     = var.postgres_url
  }
  setting {
    namespace = "aws:elasticbeanstalk:application:environment"
    name      = "POSTGRES_USER"
    value     = var.postgres_user
  }
  setting {
    namespace = "aws:elasticbeanstalk:application:environment"
    name      = "POSTGRES_PASSWORD"
    value     = var.postgres_password
  }

  # VPC and subnet settings
  setting {
    namespace = "aws:ec2:vpc"
    name      = "VPCId"
    value     = var.vpc_id
  }
  # Use private subnets for the EC2 instances
  setting {
    namespace = "aws:ec2:vpc"
    name      = "Subnets"
    value     = join(",", var.private_subnets)
  }
  # Use public subnets for the load balancer
  setting {
    namespace = "aws:ec2:vpc"
    name      = "ELBSubnets"
    value     = join(",", var.public_subnets)
  }

  # Security groups for the instances (must be under autoscaling:launchconfiguration)
  setting {
    namespace = "aws:autoscaling:launchconfiguration"
    name      = "SecurityGroups"
    value     = join(",", var.security_group_ids)
  }

  # Instance Profile setting
  setting {
    namespace = "aws:autoscaling:launchconfiguration"
    name      = "IamInstanceProfile"
    value     = aws_iam_instance_profile.beanstalk_instance_profile.name
  }

  # Specify the load balancer type (Application Load Balancer)
  setting {
    namespace = "aws:elasticbeanstalk:environment"
    name      = "LoadBalancerType"
    value     = "application"
  }

  tags = {
    Name    = local.environment_name
    Env     = var.environment
    Team    = var.team_name
    Purpose = "SpringBootApp"
  }
}
