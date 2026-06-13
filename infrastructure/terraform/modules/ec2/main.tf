resource "aws_instance" "this" {
  ami                    = var.ami_id
  instance_type          = var.instance_type
  subnet_id              = var.subnet_id
  vpc_security_group_ids = var.security_group_ids
  iam_instance_profile   = aws_iam_instance_profile.ssm_profile.name

  root_block_device {
    volume_type           = "gp3"
    volume_size           = 20
    delete_on_termination = true
    encrypted             = true

    tags = {
      Name = "${var.project}-${var.env}-ec2-root"
    }
  }

  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"  # IMDSv2
    http_put_response_hop_limit = 1
  }

  tags = {
    Name = "${var.project}-${var.env}-ec2"
  }
}

# SSM用のIAMロール
resource "aws_iam_role" "ssm_role" {
  name = "${var.project}-${var.env}-ssm-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      },
    ]
  })
}

# AWS管理ポリシー（SSM用）をアタッチ
resource "aws_iam_role_policy_attachment" "ssm_managed" {
  role       = aws_iam_role.ssm_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

# IAM インスタンスプロファイル（EC2に紐付ける器）
resource "aws_iam_instance_profile" "ssm_profile" {
  name = "${var.project}-${var.env}-ssm-profile"
  role = aws_iam_role.ssm_role.name
}
