output "public_ip" {
  description = "Elastic IP of the host"
  value       = aws_eip.this.public_ip
}

output "ssh_command" {
  value = "ssh -i <path-to-private-key> ubuntu@${aws_eip.this.public_ip}"
}

output "gateway_url" {
  value = "http://${aws_eip.this.public_ip}:8080"
}

output "eureka_dashboard_url" {
  value = "http://${aws_eip.this.public_ip}:8761"
}

output "config_server_url" {
  value = "http://${aws_eip.this.public_ip}:8888"
}

output "generated_db_passwords" {
  description = "Auto-generated passwords, only populated for values you didn't override in tfvars"
  value = {
    agent_db      = local.agent_db_password
    deployment_db = local.deployment_db_password
    customer_db   = local.customer_db_password
    auth_db       = local.auth_db_password
    mongo_root    = local.mongo_root_password
  }
  sensitive = true
}
