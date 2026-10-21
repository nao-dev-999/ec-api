resource "aws_db_subnet_group" "this" {
  name        = "${var.project}-${var.env}-rds-subnet-group"
  subnet_ids  = var.subnet_ids
  description = "RDS subnet group for ${var.project} ${var.env}"

  tags = {
    Name = "${var.project}-${var.env}-rds-subnet-group"
  }
}

resource "aws_db_instance" "this" {
  identifier                  = var.identifier
  engine                      = var.engine
  engine_version              = var.engine_version
  instance_class              = var.instance_class
  allocated_storage           = 20
  max_allocated_storage       = var.max_allocated_storage
  storage_type                = "gp3"
  db_name                     = var.database_name
  username                    = var.master_username
  manage_master_user_password = true
  db_subnet_group_name        = aws_db_subnet_group.this.name
  vpc_security_group_ids      = [aws_security_group.rds.id]
  skip_final_snapshot         = var.skip_final_snapshot
  final_snapshot_identifier   = "${var.identifier}-final-snapshot"
  deletion_protection         = var.deletion_protection
  publicly_accessible         = false
  multi_az                    = var.multi_az
  port                        = 5432
  backup_retention_period     = var.backup_retention_period
  backup_window               = "17:00-17:30" # UTC。JSTでは深夜2:00-2:30
  copy_tags_to_snapshot       = true

  maintenance_window         = "sun:18:00-sun:19:00" # UTC。JSTでは月曜3:00-4:00
  auto_minor_version_upgrade = false

  performance_insights_enabled          = true
  performance_insights_retention_period = 7 # 無料枠は7日

  parameter_group_name = aws_db_parameter_group.this.name

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

  lifecycle {
    ignore_changes = [
      engine_version, # auto_minor_version_upgradeによる自動更新の差分を無視
      password,       # Secrets Managerのローテーションによる差分を無視
    ]
  }
}

# インスタンスの起動/停止をTerraformで管理する(検証環境のスリープ用)。
# instance_state = null の環境では作成せず、従来どおり常時起動とする。
# 注意: AWSの仕様で、停止したインスタンスは7日後に自動で起動する。再度 terraform apply すると停止し直す。
resource "aws_rds_instance_state" "this" {
  count = var.instance_state == null ? 0 : 1

  identifier = aws_db_instance.this.identifier
  state      = var.instance_state
}

resource "aws_db_parameter_group" "this" {
  name   = "${var.project}-${var.env}-pg-params"
  family = "${var.engine}${split(".", var.engine_version)[0]}" # engineのメジャーバージョンに合わせて動的に組み立てる

  # スロークエリログ: 指定ミリ秒以上かかったクエリを記録する
  parameter {
    name  = "log_min_duration_statement"
    value = "1000"
  }

  # 監査ログ: pgaudit拡張をプリロードする(static。再起動が必要)
  parameter {
    name         = "shared_preload_libraries"
    value        = "pgaudit"
    apply_method = "pending-reboot"
  }

  # 監査対象: DDL(スキーマ変更)とWRITE(更新系)を記録する
  parameter {
    name  = "pgaudit.log"
    value = "ddl,write"
  }
}

resource "aws_db_event_subscription" "this" {
  name        = "${var.project}-${var.env}-rds-events"
  sns_topic   = var.alarm_sns_topic_arn
  source_type = "db-instance"
  source_ids  = [aws_db_instance.this.identifier]

  event_categories = [
    "failover",
    "failure",
    "low storage",
    "maintenance",
    "notification",
  ]
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
