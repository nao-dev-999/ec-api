output "cluster_name" {
  value = aws_ecs_cluster.this.name
}

output "cluster_arn" {
  value = aws_ecs_cluster.this.arn
}

output "service_name" {
  value = aws_ecs_service.app.name
}

output "flyway_task_definition_arn" {
  value = aws_ecs_task_definition.flyway.arn
}

output "flyway_task_definition_family" {
  value = aws_ecs_task_definition.flyway.family
}

output "ecs_sg_id" {
  value = aws_security_group.ecs.id
}

output "task_execution_role_arn" {
  value = aws_iam_role.task_execution.arn
}

output "task_role_arn" {
  value = aws_iam_role.task.arn
}

output "redis_host" {
  # enable_redis = false の間はnull
  value = one(aws_elasticache_replication_group.this[*].primary_endpoint_address)
}

output "redis_cluster_ids" {
  # レプリケーショングループを構成する全ノードのIDを公開する。
  # AWS/ElastiCacheの標準メトリクス（EngineCPUUtilization等）はノード単位（CacheClusterId）でしか
  # 発行されず、レプリケーショングループ全体やプライマリ/レプリカの役割を表すディメンションは存在しないため、
  # 先頭ノードだけでなく全ノードをfor_eachで監視できるよう、リストとして公開する。
  #
  # aws_elasticache_replication_group.this.member_clusters はapply後にしか確定しない値のため、
  # これをそのままalarmsモジュール側のfor_eachに渡すと、新規作成時のplanが
  # "Invalid for_each argument" エラーで失敗する。AWSはnum_cache_clustersで
  # ノードを作成する際、replication_group_idに3桁の連番("-001"、"-002"...)を
  # 付与したIDを自動採番する仕様のため、ここではその命名規則に基づいてplan時点で
  # 確定できる値としてノードIDを組み立てる。
  description = "CloudWatchメトリクスのCacheClusterIdディメンションに使うクラスタID一覧"
  value = [
    for i in range(1, var.redis_num_cache_clusters + 1) :
    format("%s-%03d", local.redis_replication_group_id, i)
  ]
}

output "redis_auth_token_secret_arn" {
  value = aws_secretsmanager_secret.redis_auth_token.arn
}

output "db_debug_task_definition_family" {
  value = aws_ecs_task_definition.db_debug.family
}

output "batch_task_definition_arn" {
  value = aws_ecs_task_definition.batch.arn
}

output "batch_task_definition_family" {
  value = aws_ecs_task_definition.batch.family
}

output "batch_state_machine_arn" {
  value = aws_sfn_state_machine.batch_orchestrator.arn
}

output "app_log_group_name" {
  value = aws_cloudwatch_log_group.app.name
}
