resource "aws_db_subnet_group" "this" {
  name        = "${var.project}-${var.env}-rds-subnet-group"
  subnet_ids  = var.subnet_ids
  description = "RDS subnet group for ${var.project} ${var.env}"

  tags = {
    Name = "${var.project}-${var.env}-rds-subnet-group"
  }
}

resource "aws_db_instance" "this" {
  identifier              = var.identifier
  engine                  = var.engine
  engine_version          = var.engine_version
  instance_class          = var.instance_class
  allocated_storage       = 20
  storage_type            = "gp3"
  db_name                 = var.database_name
  username                = var.master_username
  manage_master_user_password = true
  db_subnet_group_name    = aws_db_subnet_group.this.name
  vpc_security_group_ids  = var.security_group_ids
  skip_final_snapshot     = true
  final_snapshot_identifier = "${var.identifier}-final-snapshot"
  deletion_protection     = false
  publicly_accessible     = false
  multi_az                = false
  port                    = 5432

  tags = {
    Name = var.identifier
  }
}
