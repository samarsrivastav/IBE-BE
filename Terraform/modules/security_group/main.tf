resource "aws_security_group" "this" {
  name        = "${var.team_name}-beanstalk-sg-${var.environment}"
  description = "Security group for EB instances"
  vpc_id      = var.vpc_id

  ingress {
    description = "Allow traffic on port 8080"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]  # Adjust as needed for production
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
