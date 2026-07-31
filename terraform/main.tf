data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

data "aws_ami" "ubuntu" {
  most_recent = true
  owners      = ["099720109477"] # Canonical

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

resource "random_password" "agent_db" {
  count   = var.agent_db_password == null ? 1 : 0
  length  = 20
  special = false
}

resource "random_password" "deployment_db" {
  count   = var.deployment_db_password == null ? 1 : 0
  length  = 20
  special = false
}

resource "random_password" "customer_db" {
  count   = var.customer_db_password == null ? 1 : 0
  length  = 20
  special = false
}

resource "random_password" "auth_db" {
  count   = var.auth_db_password == null ? 1 : 0
  length  = 20
  special = false
}

resource "random_password" "mongo_root" {
  count   = var.mongo_root_password == null ? 1 : 0
  length  = 20
  special = false
}

locals {
  agent_db_password      = coalesce(var.agent_db_password, try(random_password.agent_db[0].result, null))
  deployment_db_password = coalesce(var.deployment_db_password, try(random_password.deployment_db[0].result, null))
  customer_db_password   = coalesce(var.customer_db_password, try(random_password.customer_db[0].result, null))
  auth_db_password       = coalesce(var.auth_db_password, try(random_password.auth_db[0].result, null))
  mongo_root_password    = coalesce(var.mongo_root_password, try(random_password.mongo_root[0].result, null))
}

resource "aws_key_pair" "this" {
  key_name   = "${var.project_name}-key"
  public_key = file(pathexpand(var.public_key_path))
}

resource "aws_security_group" "this" {
  name        = "${var.project_name}-sg"
  description = "AgentHub EC2 host"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.allowed_ssh_cidr]
  }

  ingress {
    description = "Gateway (public API entrypoint)"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Admin/debug ports: config-service, discovery-service, domain services"
    from_port   = 8081
    to_port     = 8086
    protocol    = "tcp"
    cidr_blocks = [var.allowed_ssh_cidr]
  }

  ingress {
    description = "Config server"
    from_port   = 8888
    to_port     = 8888
    protocol    = "tcp"
    cidr_blocks = [var.allowed_ssh_cidr]
  }

  ingress {
    description = "Eureka dashboard"
    from_port   = 8761
    to_port     = 8761
    protocol    = "tcp"
    cidr_blocks = [var.allowed_ssh_cidr]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${var.project_name}-sg"
  }
}

resource "aws_instance" "this" {
  ami                    = data.aws_ami.ubuntu.id
  instance_type          = var.instance_type
  subnet_id              = data.aws_subnets.default.ids[0]
  vpc_security_group_ids = [aws_security_group.this.id]
  key_name               = aws_key_pair.this.key_name

  root_block_device {
    volume_size = var.root_volume_size
    volume_type = "gp3"
  }

  user_data = templatefile("${path.module}/user_data.sh.tftpl", {
    github_repo_url        = var.github_repo_url
    deploy_key             = file(pathexpand(var.deploy_key_path))
    git_branch             = var.git_branch
    app_profile            = var.app_profile
    agent_db_name          = var.agent_db_name
    agent_db_user          = var.agent_db_user
    agent_db_password      = local.agent_db_password
    deployment_db_name     = var.deployment_db_name
    deployment_db_user     = var.deployment_db_user
    deployment_db_password = local.deployment_db_password
    customer_db_name       = var.customer_db_name
    customer_db_user       = var.customer_db_user
    customer_db_password   = local.customer_db_password
    auth_db_name           = var.auth_db_name
    auth_db_user           = var.auth_db_user
    auth_db_password       = local.auth_db_password
    mongo_db_name          = var.mongo_db_name
    mongo_root_user        = var.mongo_root_user
    mongo_root_password    = local.mongo_root_password
    google_api_key         = var.google_api_key
  })

  tags = {
    Name = "${var.project_name}-host"
  }
}

resource "aws_eip" "this" {
  instance = aws_instance.this.id
  domain   = "vpc"

  tags = {
    Name = "${var.project_name}-eip"
  }
}
