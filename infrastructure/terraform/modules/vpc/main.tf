data "aws_caller_identity" "current" {}
data "aws_region" "current" {}

resource "aws_vpc" "this" {
  cidr_block           = var.vpc_cidr
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Name = "${var.project}-${var.env}-vpc"
  }
}

resource "aws_internet_gateway" "this" {
  vpc_id = aws_vpc.this.id

  tags = {
    Name = "${var.project}-${var.env}-igw"
  }
}

# ---------------------------------------------------------------------------
# Public Subnets
# ---------------------------------------------------------------------------
resource "aws_subnet" "public" {
  count = length(var.public_subnet_cidrs)

  vpc_id                  = aws_vpc.this.id
  cidr_block              = var.public_subnet_cidrs[count.index]
  availability_zone       = var.availability_zones[count.index]
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.project}-${var.env}-public-${count.index + 1}"
    Tier = "public"
  }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.this.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.this.id
  }

  tags = {
    Name = "${var.project}-${var.env}-rtb-public"
  }
}

resource "aws_route_table_association" "public" {
  count = length(aws_subnet.public)

  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

# ---------------------------------------------------------------------------
# Private Subnets
# ---------------------------------------------------------------------------
resource "aws_subnet" "private" {
  count = length(var.private_subnet_cidrs)

  vpc_id            = aws_vpc.this.id
  cidr_block        = var.private_subnet_cidrs[count.index]
  availability_zone = var.availability_zones[count.index]

  tags = {
    Name = "${var.project}-${var.env}-private-${count.index + 1}"
    Tier = "private"
  }
}

# NAT Gateway (1台 / 冗長化が必要な場合は count を増やす)
resource "aws_eip" "nat" {
  domain = "vpc"

  tags = {
    Name = "${var.project}-${var.env}-eip-nat"
  }
}

resource "aws_nat_gateway" "this" {
  allocation_id = aws_eip.nat.id
  subnet_id     = aws_subnet.public[0].id

  tags = {
    Name = "${var.project}-${var.env}-natgw"
  }

  depends_on = [aws_internet_gateway.this]
}

resource "aws_route_table" "private" {
  vpc_id = aws_vpc.this.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.this.id
  }

  tags = {
    Name = "${var.project}-${var.env}-rtb-private"
  }
}

resource "aws_route_table_association" "private" {
  count = length(aws_subnet.private)

  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private.id
}

# ---------------------------------------------------------------------------
# デフォルトセキュリティグループ (EC2.2 / FSBP)
# VPC作成時にAWSが自動生成する全許可のデフォルトSGを、全ルール削除の上でterraform管理下に置く。
# 誤ってこのSGにアタッチされたリソースがあっても通信できないようにする「使わせない」ためのSG。
# ---------------------------------------------------------------------------
resource "aws_default_security_group" "this" {
  vpc_id = aws_vpc.this.id

  # ingress/egressブロックを空にすることで、全ルールを削除する
  tags = {
    Name = "${var.project}-${var.env}-default-sg-restricted"
  }
}

# ---------------------------------------------------------------------------
# VPC Flow Logs
# 不正通信の事後調査・異常な通信パターンの検知のため、VPC内の全トラフィックメタデータを記録する。
# WAFログと同様、CloudWatch Logs(取り込み課金+保存課金の二重コスト)は使わず、S3へ直接配信する。
# ---------------------------------------------------------------------------
resource "aws_s3_bucket" "vpc_flow_logs" {
  bucket        = "${var.project}-${var.env}-vpc-flow-logs"
  force_destroy = true

  tags = {
    Name = "${var.project}-${var.env}-vpc-flow-logs"
  }
}

# パブリックアクセスは一切許可しない（ログには送信元/宛先IP等の情報が含まれるため）
resource "aws_s3_bucket_public_access_block" "vpc_flow_logs" {
  bucket = aws_s3_bucket.vpc_flow_logs.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "vpc_flow_logs" {
  bucket = aws_s3_bucket.vpc_flow_logs.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_versioning" "vpc_flow_logs" {
  bucket = aws_s3_bucket.vpc_flow_logs.id
  versioning_configuration {
    status = "Enabled"
  }
}

# コスト削減のため、一定期間経過後にIA/Glacierへ移行し、最終的に自動削除する
resource "aws_s3_bucket_lifecycle_configuration" "vpc_flow_logs" {
  bucket = aws_s3_bucket.vpc_flow_logs.id

  rule {
    id     = "expire-vpc-flow-logs"
    status = "Enabled"

    filter {}

    transition {
      days          = 30
      storage_class = "STANDARD_IA"
    }

    transition {
      days          = 90
      storage_class = "GLACIER"
    }

    expiration {
      days = var.vpc_flow_log_retention_days
    }

    # versioning有効化に伴い、旧バージョンも同様に整理する
    noncurrent_version_expiration {
      noncurrent_days = var.vpc_flow_log_retention_days
    }

    abort_incomplete_multipart_upload {
      days_after_initiation = 7
    }
  }
}

# VPC Flow LogsをS3へ直接配信するための権限。CloudWatch Logs時代のIAMロールに代わり、
# ログ配信サービス(delivery.logs.amazonaws.com。VPC Flow Logs/Route 53 Resolverクエリログ等、
# 各種vended logのS3配信で共通のサービスプリンシパル)からの書き込みをバケットポリシーで許可する。
# 特定のFlow LogのARNに絞ると、aws_flow_log.this がこのバケットポリシーに
# depends_onで依存している関係上、循環参照になってしまうため、
# サービス種別(logs)+アカウント+リージョンのワイルドカードで絞り込む（ALBログバケットと同じ考え方）。
resource "aws_s3_bucket_policy" "vpc_flow_logs" {
  bucket = aws_s3_bucket.vpc_flow_logs.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid       = "AWSLogDeliveryWrite"
        Effect    = "Allow"
        Principal = { Service = "delivery.logs.amazonaws.com" }
        Action    = "s3:PutObject"
        Resource  = "${aws_s3_bucket.vpc_flow_logs.arn}/vpc-flow-logs/AWSLogs/${data.aws_caller_identity.current.account_id}/*"
        Condition = {
          StringEquals = {
            "s3:x-amz-acl"      = "bucket-owner-full-control"
            "aws:SourceAccount" = data.aws_caller_identity.current.account_id
          }
          ArnLike = {
            "aws:SourceArn" = "arn:aws:logs:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:*"
          }
        }
      },
      {
        Sid       = "AWSLogDeliveryAclCheck"
        Effect    = "Allow"
        Principal = { Service = "delivery.logs.amazonaws.com" }
        Action    = "s3:GetBucketAcl"
        Resource  = aws_s3_bucket.vpc_flow_logs.arn
        Condition = {
          StringEquals = {
            "aws:SourceAccount" = data.aws_caller_identity.current.account_id
          }
          ArnLike = {
            "aws:SourceArn" = "arn:aws:logs:${data.aws_region.current.name}:${data.aws_caller_identity.current.account_id}:*"
          }
        }
      }
    ]
  })
}

resource "aws_flow_log" "this" {
  vpc_id                = aws_vpc.this.id
  traffic_type          = "ALL"
  log_destination_type  = "s3"
  log_destination       = "${aws_s3_bucket.vpc_flow_logs.arn}/vpc-flow-logs/"

  # Athenaでのクエリコスト削減のためParquet形式、Hive形式パーティションでパーティションプルーニングを効かせる
  destination_options {
    file_format                = "parquet"
    per_hour_partition         = true
    hive_compatible_partitions = true
  }

  tags = {
    Name = "${var.project}-${var.env}-vpc-flow-log"
  }

  depends_on = [aws_s3_bucket_policy.vpc_flow_logs]
}
