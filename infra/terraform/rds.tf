resource "aws_security_group" "rds_sg" {
  name        = "${var.cluster_name}-rds-sg"
  description = "Allow PostgreSQL traffic from EKS worker nodes"
  vpc_id      = module.vpc.vpc_id

  ingress {
    description     = "PostgreSQL from EKS managed nodes"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [module.eks.node_security_group_id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Project   = "beer-catalogue-api"
    ManagedBy = "terraform"
  }
}

resource "aws_db_subnet_group" "beer_catalogue" {
  name       = "${var.cluster_name}-db-subnet-group"
  subnet_ids = module.vpc.private_subnets

  tags = {
    Project   = "beer-catalogue-api"
    ManagedBy = "terraform"
  }
}

resource "aws_db_instance" "beer_catalogue" {
  identifier     = var.cluster_name
  engine         = "postgres"
  engine_version = "16"
  instance_class = var.db_instance_class

  allocated_storage = 20

  db_name  = var.db_name
  username = var.db_username
  password = var.db_password

  db_subnet_group_name   = aws_db_subnet_group.beer_catalogue.name
  vpc_security_group_ids = [aws_security_group.rds_sg.id]

  publicly_accessible = false

  multi_az                = false
  skip_final_snapshot     = true
  backup_retention_period = 0

  tags = {
    Project     = "beer-catalogue-api"
    Environment = "eval"
    ManagedBy   = "terraform"
  }
}
