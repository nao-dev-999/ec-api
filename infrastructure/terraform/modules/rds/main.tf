resource "aws_db_subnet_group" "this" {
  name        = "${var.project}-${var.env}-rds-subnet-group"
  subnet_ids  = var.subnet_ids
  description = "RDS subnet group for ${var.project} ${var.env}"

  tags = {
    Name = "${var.project}-${var.env}-rds-subnet-group"
  }
}

resource "aws_db_instance" "this" {
  identifier                   = var.identifier
  engine                       = var.engine
  engine_version                = var.engine_version
  instance_class                = var.instance_class
  allocated_storage             = 20
  max_allocated_storage          = var.max_allocated_storage
  storage_type                  = "gp3"
  db_name                       = var.database_name
  username                      = var.master_username
  manage_master_user_password    = true
  db_subnet_group_name           = aws_db_subnet_group.this.name
  vpc_security_group_ids         = [aws_security_group.rds.id]
  skip_final_snapshot            = true
  final_snapshot_identifier      = "${var.identifier}-final-snapshot"
  deletion_protection            = false
  publicly_accessible            = false
  multi_az                       = false
  port                           = 5432
  backup_retention_period        = var.backup_retention_period

  # RDS.3(FSBP): 保管時暗号化。デフォルトのAWS管理KMSキー(aws/rds)を使用する
  storage_encrypted = true

  # RDS.6(FSBP): Enhanced Monitoring。60秒間隔でOS/プロセスレベルのメトリクスを取得する
  monitoring_interval = 60
  monitoring_role_arn = aws_iam_role.rds_enhanced_monitoring.arn

  # RDS.9(FSBP): DBログをCloudWatch Logsへエクスポートする(PostgreSQLエンジンの対応ログタイプ)
  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]

  tags = {
    Name = var.identifier
  }
}

# ---------------------------------------------------------------------------
# IAM Role for RDS Enhanced Monitoring
# ---------------------------------------------------------------------------
resource "aws_iam_role" "rds_enhanced_monitoring" {
  name = "${var.project}-${var.env}-rds-enhanced-monitoring-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "monitoring.rds.amazonaws.com" }
      Action    = "sts:AssumeRole"
    }]
  })

  tags = {
    Name = "${var.project}-${var.env}-rds-enhanced-monitoring-role"
  }
}

resource "aws_iam_role_policy_attachment" "rds_enhanced_monitoring" {
  role       = aws_iam_role.rds_enhanced_monitoring.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonRDSEnhancedMonitoringRole"
}

resource "aws_security_group" "rds" {
  name        = "${var.project}-${var.env}-rds-sg"
  description = "Security group for rds"
  vpc_id      = var.vpc_id

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [var.ecs_sg_id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project}-${var.env}-rds-sg"
  }
}
