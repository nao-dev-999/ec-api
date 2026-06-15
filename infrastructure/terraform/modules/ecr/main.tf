resource "aws_ecr_repository" "app" {
  name                 = "${var.project}-${var.env}-app"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name    = "${var.project}-${var.env}-app"
    Project = var.project
    Env     = var.env
  }
}

# Flywayマイグレーション用リポジトリ（appと同イメージを使い回す想定だが分離も可）
resource "aws_ecr_repository" "flyway" {
  name                 = "${var.project}-${var.env}-flyway"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Name    = "${var.project}-${var.env}-flyway"
    Project = var.project
    Env     = var.env
  }
}

# 古いイメージを自動削除（直近5世代を保持）
resource "aws_ecr_lifecycle_policy" "app" {
  repository = aws_ecr_repository.app.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep last 5 images"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 5
      }
      action = { type = "expire" }
    }]
  })
}

resource "aws_ecr_lifecycle_policy" "flyway" {
  repository = aws_ecr_repository.flyway.name

  policy = jsonencode({
    rules = [{
      rulePriority = 1
      description  = "Keep last 5 images"
      selection = {
        tagStatus   = "any"
        countType   = "imageCountMoreThan"
        countNumber = 5
      }
      action = { type = "expire" }
    }]
  })
}
