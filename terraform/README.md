# AgentHub — Terraform (AWS)

Provisions a single EC2 instance in your default VPC, installs Docker, clones
this repo, and runs the existing `docker-compose.yml` as-is (all 7 services +
4 Postgres containers + MongoDB, same as local dev). State lives locally, so
`terraform destroy` tears everything back down.

## Prerequisites

- Terraform >= 1.5
- AWS credentials are configured (`aws configure`, or env vars / SSO profile) with
  permission to manage EC2, security groups, and key pairs
- A local SSH keypair (defaults to `~/.ssh/id_ed25519.pub` — generate one with
  `ssh-keygen -t ed25519` if you don't have one)
- A GitHub deploy key, since the repo is private. Generate one and register it
  as a **read-only** Deploy Key on the repo (Settings → Deploy keys → Add deploy key):
  ```bash
  ssh-keygen -t ed25519 -f ~/.ssh/agenthub_deploy_key -N "" -C agenthub-terraform-deploy-key
  cat ~/.ssh/agenthub_deploy_key.pub   # paste this in as the deploy key
  ```
  The instance uses the private half (`deploy_key_path`, default
  `~/.ssh/agenthub_deploy_key`) to `git clone` over SSH at boot.

## Deploy

```bash
cd terraform
cp terraform.tfvars.example terraform.tfvars
# edit terraform.tfvars: set allowed_ssh_cidr to your IP (curl ifconfig.me),
# set google_api_key

terraform init
terraform apply
```

Boot and build take a few minutes (Maven builds all seven (7) services on the first boot).
Watch progress with:

```bash
ssh -i ~/.ssh/id_ed25519 ubuntu@$(terraform output -raw public_ip) \
  'tail -f /var/log/agenthub-bootstrap.log'
```

Then check:

```bash
terraform output gateway_url
curl $(terraform output -raw gateway_url)/actuator/health   # if exposed via gateway
```

or SSH in and run `docker compose ps` from `/opt/agenthub`.

## Redeploy after a code change

The instance only clones the repo once, at boot. To pick up new commits:

```bash
ssh -i ~/.ssh/id_ed25519 ubuntu@$(terraform output -raw public_ip)
cd /opt/agenthub
git pull
docker compose up --build -d
```

## Destroy

```bash
terraform destroy
```

Removes the instance, EIP, security group, and key pair. Nothing else in
your AWS account is touched.

## Notes / known limitations

- Newer AWS accounts restrict `RunInstances` to free-tier-eligible instance
  types only. `instance_type` defaults to `m7i-flex.large` (8GB/2vCPU, free-
  tier-eligible) for that reason. If you hit `InvalidParameterCombination:
  ... not eligible for Free Tier`, run
  `aws ec2 describe-instance-types --filters Name=free-tier-eligible,Values=true`
  to see what your account can launch.
- `app_profile` defaults to `dev` (auto-creates schema on boot). Switching to
  `prod` requires the schema to already exist — see the main README.
- Ports 8081–8086, 8888, 8761 are restricted to `allowed_ssh_cidr` (debug/
  admin only); only 8080 (gateway) is open to the internet.
- Auto-generated DB passwords are visible via
  `terraform output -json generated_db_passwords`.
