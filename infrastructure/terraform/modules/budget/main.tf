# ---------------------------------------------------------------------------
# AWS Budgets: テスト環境での想定外課金を検知する
# ---------------------------------------------------------------------------
resource "aws_budgets_budget" "monthly_cost" {
  name         = "${var.project}-${var.env}-monthly-cost"
  budget_type  = "COST"
  limit_amount = var.monthly_limit_usd
  limit_unit   = "USD"
  time_unit    = "MONTHLY"

  # メールアドレス未設定の場合は予算のみ作成し、通知は行わない（通知ブロックは購読者0件だとAPIエラーになるため）
  dynamic "notification" {
    for_each = length(var.notification_emails) > 0 ? [80, 100] : []
    content {
      comparison_operator        = "GREATER_THAN"
      threshold                  = notification.value
      threshold_type             = "PERCENTAGE"
      notification_type          = notification.value < 100 ? "ACTUAL" : "FORECASTED"
      subscriber_email_addresses = var.notification_emails
    }
  }
}
