# ドメイン未取得のため、自己署名証明書をACMにインポートして使う。
# ブラウザからは「信頼されていない証明書」として警告が出る（CA署名ではないため）。
# ドメインを取得したら、Route53 + ACM(DNS検証)の正式な証明書に切り替えること。
resource "tls_private_key" "this" {
  algorithm = "RSA"
  rsa_bits  = 2048
}

resource "tls_self_signed_cert" "this" {
  private_key_pem = tls_private_key.this.private_key_pem

  subject {
    common_name  = "${var.project}-${var.env}.local"
    organization = var.project
  }

  validity_period_hours = 8760 # 365日

  allowed_uses = [
    "key_encipherment",
    "digital_signature",
    "server_auth",
  ]
}

resource "aws_acm_certificate" "self_signed" {
  private_key      = tls_private_key.this.private_key_pem
  certificate_body = tls_self_signed_cert.this.cert_pem

  lifecycle {
    create_before_destroy = true
  }

  tags = {
    Name = "${var.project}-${var.env}-self-signed"
  }
}
