variable "aws_region" {
  description = "AWS region to deploy into"
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Name prefix used to tag/name all resources"
  type        = string
  default     = "agenthub"
}

variable "instance_type" {
  description = "EC2 instance type. Needs enough RAM for 7 JVMs + 4 Postgres + Mongo containers. Defaults to m7i-flex.large (8GB) since newer AWS accounts restrict RunInstances to free-tier-eligible types only — check with 'aws ec2 describe-instance-types --filters Name=free-tier-eligible,Values=true' if this errors."
  type        = string
  default     = "m7i-flex.large"
}

variable "root_volume_size" {
  description = "Root EBS volume size in GB (Maven builds + Docker images need room)"
  type        = number
  default     = 30
}

variable "allowed_ssh_cidr" {
  description = "CIDR allowed to reach SSH (22) and the admin/debug ports (Eureka, Config Server, per-service ports). Use your own IP, e.g. 203.0.113.5/32."
  type        = string
}

variable "public_key_path" {
  description = "Path to a local SSH public key to install on the instance"
  type        = string
  default     = "~/.ssh/id_ed25519.pub"
}

variable "github_repo_url" {
  description = "Git URL the instance clones at boot"
  type        = string
  default     = "https://github.com/polloparatodos/SemesterProject.git"
}

variable "git_branch" {
  description = "Branch to deploy"
  type        = string
  default     = "master"
}

variable "app_profile" {
  description = "Spring profile for the domain services (dev or prod). dev auto-creates the schema on first boot; prod requires the schema to already exist."
  type        = string
  default     = "dev"
}

variable "google_api_key" {
  description = "GOOGLE_API_KEY passed to agent-catalog-service"
  type        = string
  sensitive   = true
  default     = ""
}

variable "agent_db_name" {
  type    = string
  default = "agentdb"
}

variable "agent_db_user" {
  type    = string
  default = "agentuser"
}

variable "agent_db_password" {
  description = "Leave null to auto-generate a random password"
  type        = string
  sensitive   = true
  default     = null
}

variable "deployment_db_name" {
  type    = string
  default = "deploymentdb"
}

variable "deployment_db_user" {
  type    = string
  default = "deploymentuser"
}

variable "deployment_db_password" {
  description = "Leave null to auto-generate a random password"
  type        = string
  sensitive   = true
  default     = null
}

variable "customer_db_name" {
  type    = string
  default = "customerdb"
}

variable "customer_db_user" {
  type    = string
  default = "customeruser"
}

variable "customer_db_password" {
  description = "Leave null to auto-generate a random password"
  type        = string
  sensitive   = true
  default     = null
}

variable "auth_db_name" {
  type    = string
  default = "authdb"
}

variable "auth_db_user" {
  type    = string
  default = "authuser"
}

variable "auth_db_password" {
  description = "Leave null to auto-generate a random password"
  type        = string
  sensitive   = true
  default     = null
}

variable "mongo_db_name" {
  type    = string
  default = "mongodb"
}

variable "mongo_root_user" {
  type    = string
  default = "mongouser"
}

variable "mongo_root_password" {
  description = "Leave null to auto-generate a random password"
  type        = string
  sensitive   = true
  default     = null
}
