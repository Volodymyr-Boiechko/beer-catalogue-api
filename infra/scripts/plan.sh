#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TF_DIR="$(cd "${SCRIPT_DIR}/../terraform" && pwd)"

echo "======================================================================"
echo "  beer-catalogue-api  —  Terraform Plan (read-only preview)"
echo "======================================================================"
echo

for cmd in terraform aws; do
  if ! command -v "$cmd" &>/dev/null; then
    echo "ERROR: '$cmd' is not installed or not on PATH. Aborting." >&2
    exit 1
  fi
done

echo ">>> Verifying AWS credentials..."
if ! aws sts get-caller-identity &>/dev/null; then
  echo "ERROR: AWS credentials missing or expired." \
       "Run 'aws configure' or export AWS_* environment variables." >&2
  exit 1
fi
echo "    Identity: $(aws sts get-caller-identity --query Arn --output text)"

PLAN_PASSWORD="${DB_PASSWORD:-change-me}"
if [[ "$PLAN_PASSWORD" == "change-me" ]]; then
  echo
  echo "    Note: DB_PASSWORD is not set — using placeholder 'change-me'."
  echo "    This is safe for plan; export DB_PASSWORD before running deploy.sh."
fi

echo
echo ">>> Initialising Terraform..."
cd "$TF_DIR"
terraform init -input=false

echo
echo ">>> Validating configuration..."
terraform validate

echo
echo ">>> Running terraform plan..."
echo "    (no resources will be created or modified)"
echo
terraform plan -var="db_password=${PLAN_PASSWORD}"
