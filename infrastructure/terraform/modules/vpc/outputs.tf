output "vpc_id" {
  value = aws_vpc.this.id
}

output "public_subnet_ids" {
  value = aws_subnet.public[*].id
}

output "private_subnet_ids" {
  value = aws_subnet.private[*].id
}

output "public_route_table_id" {
  value = aws_route_table.public.id
}

# output "private_route_table_id" {
#   value = aws_route_table.private.id
# }

# 既存のコードの後に追記
output "db_sg_id" {
  description = "Security Group ID for RDS"
  value       = aws_security_group.db.id
}

output "web_sg_id" {
  description = "Security Group ID for EC2"
  value       = aws_security_group.web.id
}

output "alb_sg_id" {
  value = aws_security_group.alb.id
}
