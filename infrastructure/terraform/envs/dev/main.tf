# VPC Module
module "vpc" {
  source = "../../modules/vpc"

  project              = var.project
  env                  = var.env
  vpc_cidr             = var.vpc_cidr
  public_subnet_cidrs  = var.public_subnet_cidrs
  private_subnet_cidrs = var.private_subnet_cidrs
  availability_zones   = var.availability_zones
  nat_gateway_count    = var.nat_gateway_count
  enable_nat_gateway   = !var.sleep_mode
}

# RDS Module (db.t4g.nano)
module "rds" {
  source          = "../../modules/rds" # 既に変更済み
  project         = var.project
  env             = var.env
  aws_region      = var.aws_region
  identifier      = "${var.project}-${var.env}-rds"
  engine          = var.rds_engine
  engine_version  = var.rds_engine_version
  instance_class  = var.rds_instance_class
  database_name   = var.rds_database_name
  master_username = var.rds_master_username
  vpc_id          = module.vpc.vpc_id
  subnet_ids      = module.vpc.private_subnet_ids
  ecs_sg_id       = module.ecs.ecs_sg_id

  multi_az            = var.rds_multi_az
  skip_final_snapshot = var.rds_skip_final_snapshot
  deletion_protection = var.rds_deletion_protection
  instance_state      = var.sleep_mode ? "stopped" : "available"
  alarm_sns_topic_arn = module.alarms.sns_topic_arn

  # ecs_sg_id経由でmodule.ecsの特定リソースには暗黙に依存しているが、
  # Internet Gateway等module.vpc配下の他リソースとは依存関係が無く、
  # destroy時に並行して壊されうる(ENIが残っているとIGWのデタッチに失敗して長時間リトライする)。
  # そのため明示的にVPCモジュール全体を先に残す(destroy時は先に壊す)よう依存させる。
  depends_on = [module.vpc]
}

# ALB
module "alb" {
  source = "../../modules/alb"

  project           = var.project
  env               = var.env
  vpc_id            = module.vpc.vpc_id
  public_subnet_ids = module.vpc.public_subnet_ids
  target_port       = 8080
  health_check_path = "/actuator/health"

  enable_alb                 = !var.sleep_mode
  enable_deletion_protection = false # devでは頻繁に作り直すためfalse。本番環境ではtrueにすること

  # ALBはsubnet_ids経由でVPCモジュールの一部リソース(サブネット)には暗黙に依存するが、
  # Internet Gatewayとは依存関係が無い。destroy時にALB(のENI)がまだ残っている間に
  # IGWのデタッチが試みられ、DependencyViolationのリトライで長時間詰まることがあるため、
  # VPCモジュール全体より確実に先に破棄されるよう明示的に依存させる。
  depends_on = [module.vpc]
}

# ALB - Frontend（Next.js用。backendとはAPIを完全に分離するため別ALB・別ドメイン想定）
module "alb_frontend" {
  source = "../../modules/alb"

  project           = var.project
  env               = var.env
  name              = "frontend"
  vpc_id            = module.vpc.vpc_id
  public_subnet_ids = module.vpc.public_subnet_ids
  target_port       = 3000
  health_check_path = "/"

  enable_alb                 = !var.sleep_mode
  enable_deletion_protection = false # devでは頻繁に作り直すためfalse。本番環境ではtrueにすること

  depends_on = [module.vpc]
}

# WAF（ALBの手前でIPアドレス単位のレートベース制限）
module "waf" {
  source = "../../modules/waf"

  project = var.project
  env     = var.env
  enabled = !var.sleep_mode
  alb_arn = module.alb.alb_arn
}

# ECR
module "ecr" {
  source  = "../../modules/ecr"
  project = var.project
  env     = var.env
}

# ---------------------------------------------------------------------------
# 現在デプロイ済みのapp/batchタスク定義の読み取り
# ---------------------------------------------------------------------------
# イメージのタグ(コミットSHA)はCodePipelineが決めるため、Terraformはタグを固定せず
# 「今動いているイメージ」をAWSから読み取ってタスク定義に入れる。これにより、
# Terraformでタスク定義の設定(CPU/メモリ/環境変数等)を変えてapplyしたとき、
# イメージはそのままで設定だけが反映される。
#
# module.ecsの中ではなくここ(depends_onの無いルート)で読み取る: module.ecsにはモジュール単位の
# depends_on = [module.vpc, module.alb, module.alb_frontend] があるため、モジュール内のdata sourceは
# それらに変更がある plan(sleep_modeの切り替え時は毎回)で読み取りがapply時まで遅れ、
# イメージがknown after applyになる(タスク定義のreplaceが毎回計画され、:latestのpreconditionも
# plan時に判定されなくなる)。ここではクラスタ名・サービス名を文字列で組み立て、管理リソースを参照しない。
#
# *_image_tag を明示した場合(初回構築時など、読み取り元が存在しないとき)はその値を優先し、読み取りは行わない。
data "aws_caller_identity" "current" {}

locals {
  ecs_cluster_name     = "${var.project}-${var.env}-cluster"
  ecs_app_service_name = "${var.project}-${var.env}-app-service"
}

# app: ECSサービスが現在使っているタスク定義
data "aws_ecs_service" "app_current" {
  count        = var.app_image_tag == null ? 1 : 0
  service_name = local.ecs_app_service_name
  cluster_arn  = "arn:aws:ecs:${var.aws_region}:${data.aws_caller_identity.current.account_id}:cluster/${local.ecs_cluster_name}"
}

data "aws_ecs_container_definition" "app_current" {
  count           = var.app_image_tag == null ? 1 : 0
  task_definition = data.aws_ecs_service.app_current[0].task_definition
  container_name  = "app"
}

# batch: サービスが無いため、ファミリーの最新ACTIVEリビジョン(Step Functionsが実行するもの)
data "aws_ecs_container_definition" "batch_current" {
  count           = var.batch_image_tag == null ? 1 : 0
  task_definition = "${var.project}-${var.env}-batch"
  container_name  = "batch"
}

locals {
  app_image   = var.app_image_tag != null ? "${module.ecr.app_repository_url}:${var.app_image_tag}" : data.aws_ecs_container_definition.app_current[0].image
  batch_image = var.batch_image_tag != null ? "${module.ecr.batch_repository_url}:${var.batch_image_tag}" : data.aws_ecs_container_definition.batch_current[0].image

  # 現在のappタスク定義の環境変数(読み取っていない場合は空)
  app_current_environment = var.app_image_tag == null ? data.aws_ecs_container_definition.app_current[0].environment : {}

  # スリープ中(Redis・frontend ALBが無い間)にappタスク定義へ入れる値。
  # ダミー値に書き換えるとスリープ移行時にタスク定義が変わり、サービスのローリング更新が
  # min/maxの0化より先に走る(NAT Gateway削除後のため新タスクはイメージpullに失敗する)。
  # これを避けるため、スリープ中は現在のタスク定義の値をそのまま使ってタスク定義を据え置く。
  # タスクは0台なので値は使われず、起こすときに実際の値へ更新される(その時点ではタスク0台のまま
  # 新リビジョンに切り替わり、その後min/maxが戻ってタスクが起動する)。
  # 読み取り元が無い場合(初回構築時など)はダミー値を使う。
  app_redis_host_while_sleeping   = lookup(local.app_current_environment, "SPRING_DATA_REDIS_HOST", "redis-disabled.invalid")
  app_cors_origins_while_sleeping = lookup(local.app_current_environment, "APP_CORS_ALLOWED_ORIGINS", "https://frontend-disabled.invalid")
}

# ECS (Fargate + ElastiCache Redis)
module "ecs" {
  source = "../../modules/ecs"

  project    = var.project
  env        = var.env
  aws_region = var.aws_region

  vpc_id             = module.vpc.vpc_id
  private_subnet_ids = module.vpc.private_subnet_ids
  alb_sg_id          = module.alb.alb_sg_id
  target_group_arn   = module.alb.target_group_arn
  frontend_alb_sg_id = module.alb_frontend.alb_sg_id

  app_image = local.app_image

  flyway_image_url = module.ecr.flyway_repository_url
  flyway_image_tag = "latest"

  db_host                = module.rds.rds_endpoint
  db_name                = var.rds_database_name
  db_password_secret_arn = module.rds.rds_secret_arn

  task_cpu    = "512"
  task_memory = "1024"
  # desired_countはlifecycleでignoreされているため、スケール範囲(min/max)を0にしてタスクを止める
  desired_count = var.sleep_mode ? 0 : 2
  min_capacity  = var.sleep_mode ? 0 : 2
  max_capacity  = var.sleep_mode ? 0 : 4

  batch_image              = local.batch_image
  batch_private_subnet_ids = module.vpc.private_subnet_ids

  batch_schedule_enabled = !var.sleep_mode
  enable_redis           = !var.sleep_mode
  # スリープ中はappタスク定義を据え置く(上のlocals参照)
  redis_host_when_disabled = local.app_redis_host_while_sleeping

  swagger_enabled = var.swagger_enabled

  # backendコンテナのCORS許可オリジン(enable_alb=falseの間はnullになるため、現在のタスク定義の値を渡して
  # タスク定義を据え置く。上のlocals参照)
  frontend_public_url = module.alb_frontend.alb_dns_name != null ? "https://${module.alb_frontend.alb_dns_name}" : local.app_cors_origins_while_sleeping

  frontend_image_url        = module.ecr.frontend_repository_url
  frontend_image_tag        = "latest"
  frontend_target_group_arn = module.alb_frontend.target_group_arn

  frontend_desired_count = var.sleep_mode ? 0 : 1
  frontend_min_capacity  = var.sleep_mode ? 0 : 1
  frontend_max_capacity  = var.sleep_mode ? 0 : 2

  # ElastiCache/Fargateタスクのように、VPC内にENIを持つリソースはvpc_id/subnet_ids経由の
  # 暗黙の依存だけではInternet Gatewayとの順序が保証されない(module.alb/module.rdsと同じ理由)。
  # destroy時のIGWデタッチ詰まりを防ぐため明示的に依存させる。
  #
  # module.alb/module.alb_frontend も同様の理由で明示的に依存させる。aws_ecs_service.app/frontendは
  # target_group_arn経由でターゲットグループ(常に作成される)しか参照せず、リスナー(aws_lb_listener、
  # enable_alb=falseの間はcount=0)には暗黙の依存が無いため、ECSサービスがリスナーのLB紐付け完了を
  # 待たずに作成され、「target group does not have an associated load balancer」エラーになることがある。
  depends_on = [module.vpc, module.alb, module.alb_frontend]
}

# ALB -> ECSタスクへのアウトバウンドをアプリポート(8080)のみに限定する。
# module.alb と module.ecs は互いに参照し合う関係（ecsはalb_sg_idを、albの本来の宛先はecs_sg_id）にあり、
# モジュール間の循環参照を避けるため、このルールはenv層で両モジュールのSG IDを参照する形で定義する。
# aws_security_group.alb はインラインルールを一切持たない（modules/alb/main.tf参照）ため、
# この別リソースとの混在によるルール競合・永続的diffは発生しない。
resource "aws_vpc_security_group_egress_rule" "alb_egress_to_ecs" {
  security_group_id            = module.alb.alb_sg_id
  description                  = "Allow to ECS Fargate tasks (app port)"
  from_port                    = 8080
  to_port                      = 8080
  ip_protocol                  = "tcp"
  referenced_security_group_id = module.ecs.ecs_sg_id
}

# alb_frontend -> ECS(frontendコンテナ、3000番)も同じ理由(モジュール間循環参照回避)でenv層に定義する。
resource "aws_vpc_security_group_egress_rule" "alb_frontend_egress_to_ecs" {
  security_group_id            = module.alb_frontend.alb_sg_id
  description                  = "Allow to ECS Fargate tasks (frontend port)"
  from_port                    = 3000
  to_port                      = 3000
  ip_protocol                  = "tcp"
  referenced_security_group_id = module.ecs.ecs_sg_id
}

# CodePipeline
module "codepipeline" {
  source = "../../modules/codepipeline"

  project    = var.project
  env        = var.env
  aws_region = var.aws_region

  github_repository = var.github_repository
  github_branch     = var.github_branch

  app_repository_url    = module.ecr.app_repository_url
  flyway_repository_url = module.ecr.flyway_repository_url
  ecs_cluster_name      = module.ecs.cluster_name
  ecs_service_name      = module.ecs.service_name

  task_execution_role_arn = module.ecs.task_execution_role_arn
  task_role_arn           = module.ecs.task_role_arn

  flyway_task_definition_family = module.ecs.flyway_task_definition_family
  flyway_subnet_id              = module.vpc.private_subnet_ids[0]
  flyway_sg_id                  = module.ecs.ecs_sg_id

  batch_repository_url         = module.ecr.batch_repository_url
  batch_task_definition_family = module.ecs.batch_task_definition_family

  frontend_repository_url   = module.ecr.frontend_repository_url
  frontend_ecs_service_name = module.ecs.frontend_service_name
  # frontendのDockerビルド時にNEXT_PUBLIC_API_BASE_URLとして埋め込むbackendの公開URL
  backend_public_url = module.alb.alb_dns_name != null ? "https://${module.alb.alb_dns_name}" : "https://backend-disabled.invalid"

  manual_approval_enabled = var.manual_approval_enabled
}

# AWS Config（設定ミス・非準拠状態の検知と通知）
# 他モジュールのデプロイフローに影響を与えないよう独立して追加
#module "config" {
#  source = "../../modules/config"
#
#  project = var.project
#  env     = var.env
#
#  notification_emails = var.config_notification_emails
#}

# CloudWatch Alarms（アプリのERRORログ / ECS起動数0 / CPU使用率90%以上 / ALB応答遅延・ELB 5xx / WAFブロック多発 → SNS通知）
module "alarms" {
  source = "../../modules/alarms"

  project = var.project
  env     = var.env

  aws_region = var.aws_region
  sleep_mode = var.sleep_mode

  ecs_cluster_name   = module.ecs.cluster_name
  ecs_service_name   = module.ecs.service_name
  app_log_group_name = module.ecs.app_log_group_name

  alb_arn_suffix              = module.alb.alb_arn_suffix
  alb_target_group_arn_suffix = module.alb.target_group_arn_suffix
  rds_instance_id             = module.rds.db_instance_id
  redis_cluster_ids           = module.ecs.redis_cluster_ids

  waf_web_acl_metric_name            = module.waf.web_acl_metric_name
  waf_auth_rate_limit_metric_name    = module.waf.auth_rate_limit_metric_name
  waf_general_rate_limit_metric_name = module.waf.general_rate_limit_metric_name

  notification_emails = var.alarm_notification_emails
}

# セキュリティ監査基盤（CloudTrail: APIアクティビティ証跡 / GuardDuty: 脅威検知）
# 高深刻度のGuardDuty検出はAWS Configと同じ通知チャンネル（module.config）に集約する
#module "security_audit" {
#  source = "../../modules/security-audit"
#
#  project = var.project
#  env     = var.env
#
#  sns_topic_arn = module.config.sns_topic_arn
#}

# コスト監視（想定外課金の検知）
module "budget" {
  source = "../../modules/budget"

  project = var.project
  env     = var.env

  monthly_limit_usd   = var.monthly_budget_usd
  notification_emails = var.budget_notification_emails
}
