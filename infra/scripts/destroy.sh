#!/usr/bin/env bash
# Teardown: delete k8s resources (releases ELB), wait for ELB to disappear,
# then run terraform destroy. Order matters — skipping the ELB wait causes
# DependencyViolation and leaves orphaned resources still accruing charges.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
TF_DIR="${PROJECT_ROOT}/infra/terraform"
K8S_DIR="${PROJECT_ROOT}/infra/k8s"
RENDERED_DIR="${K8S_DIR}/.rendered"

# ─── Banner ───────────────────────────────────────────────────────────────────
echo "======================================================================"
echo "  beer-catalogue-api  —  TEARDOWN  "
echo "======================================================================"
echo "  This script is IRREVERSIBLE — all data in RDS will be deleted."
echo "======================================================================"
echo
read -r -p ">>> Type 'yes' to confirm full teardown: " CONFIRM
[[ "$CONFIRM" == "yes" ]] || { echo "Aborted."; exit 0; }

echo
echo ">>> Checking prerequisites..."
for cmd in terraform aws kubectl; do
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

if [[ -z "${DB_PASSWORD:-}" ]]; then
  echo
  echo ">>> DB_PASSWORD is not set (required for terraform destroy)."
  read -r -s -p "    Enter RDS master password (input hidden): " DB_PASSWORD
  echo
fi
[[ -z "${DB_PASSWORD:-}" ]] && { echo "ERROR: DB_PASSWORD must not be empty." >&2; exit 1; }

echo
echo ">>> [1/4] Capturing state before deletion..."
ELB_HOSTNAME=$(kubectl get svc beer-catalogue \
  -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || true)

if [[ -n "$ELB_HOSTNAME" ]]; then
  echo "    ELB hostname: ${ELB_HOSTNAME}"
  LB_NAME="${ELB_HOSTNAME%%.*}"   # abc123 from abc123.eu-west-1.elb.amazonaws.com
else
  echo "    Service 'beer-catalogue' not found or has no ELB assigned; ELB wait will be skipped."
  LB_NAME=""
fi

cd "$TF_DIR"
REGION=$(terraform output -raw region 2>/dev/null || echo "")
cd "$PROJECT_ROOT"

echo
echo ">>> [2/4] Deleting Kubernetes resources..."
K8S_APPLY_DIR="${RENDERED_DIR}"
if [[ ! -d "${K8S_APPLY_DIR}" ]]; then
  echo "    .rendered/ directory not found; falling back to k8s/ source manifests."
  K8S_APPLY_DIR="${K8S_DIR}"
fi
kubectl delete -f "${K8S_APPLY_DIR}/" --ignore-not-found
echo "    Kubernetes resources deleted. AWS is now releasing the ELB..."

if [[ -n "$LB_NAME" ]]; then
  echo
  echo ">>> [3/4] Waiting for ELB '${LB_NAME}' to be released by AWS..."
  echo "    Polling every 10s (timeout 5 min)..."
  echo

  ELAPSED=0
  TIMEOUT=300
  while true; do
    CLB_FOUND=false
    V2_FOUND=false

    if aws elb describe-load-balancers \
        --load-balancer-names "$LB_NAME" \
        --query 'LoadBalancerDescriptions[0].LoadBalancerName' \
        --output text 2>/dev/null | grep -q "$LB_NAME"; then
      CLB_FOUND=true
    fi

    V2_COUNT=$(aws elbv2 describe-load-balancers \
      --query "length(LoadBalancers[?starts_with(DNSName, '${LB_NAME}')])" \
      --output text 2>/dev/null || echo "0")
    [[ "${V2_COUNT:-0}" -gt 0 ]] && V2_FOUND=true

    if ! $CLB_FOUND && ! $V2_FOUND; then
      echo "    ELB has been fully released. Safe to proceed with terraform destroy."
      break
    fi

    ELAPSED=$((ELAPSED + 10))
    echo "    ...ELB still present (${ELAPSED}s / ${TIMEOUT}s elapsed)"

    if [[ $ELAPSED -ge $TIMEOUT ]]; then
      echo
      echo "  WARNING: ELB '${LB_NAME}' is still present after ${TIMEOUT}s."
      echo "  Terraform destroy may fail with DependencyViolation."
      echo "  Options:"
      echo "    • Wait longer: re-run this script, it will retry."
      echo "    • Manual: delete the ELB in the AWS Console, then re-run."
      echo "    • Proceed anyway (risky — destroy may partially fail)."
      echo
      read -r -p "  Type 'proceed' to run terraform destroy now anyway, or Ctrl-C to abort: " PROCEED
      [[ "$PROCEED" == "proceed" ]] || exit 1
      break
    fi

    sleep 10
  done
else
  echo ">>> [3/4] No ELB to wait for; proceeding directly to terraform destroy."
fi

echo
echo ">>> [4/4] Running terraform destroy (15–20 min)..."
cd "$TF_DIR"
terraform destroy -auto-approve -var="db_password=${DB_PASSWORD}"
cd "$PROJECT_ROOT"

echo
echo ">>> Optional: delete ECR repository 'beer-catalogue-api'?"
echo "    WARNING: this permanently deletes all Docker images stored in ECR."
read -r -p "    Delete ECR repository? [y/N]: " DELETE_ECR
if [[ "${DELETE_ECR,,}" == "y" || "${DELETE_ECR,,}" == "yes" ]]; then
  if [[ -n "$REGION" ]]; then
    echo "    Deleting ECR repository and all images..."
    aws ecr delete-repository \
      --repository-name "beer-catalogue-api" \
      --region "$REGION" \
      --force \
      --output text &>/dev/null
    echo "    ECR repository deleted."
  else
    echo "    Could not determine region (Terraform state already cleared)."
    echo "    Delete manually: aws ecr delete-repository --repository-name beer-catalogue-api --region <region> --force"
  fi
else
  echo "    Skipped. ECR images remain and will incur minimal storage charges."
fi

echo
echo "======================================================================"
echo "  Teardown complete!"
echo "======================================================================"
echo
echo "  All Terraform-managed resources have been destroyed:"
echo "    EKS cluster, worker nodes, RDS instance, VPC, NAT Gateway."
echo
echo "  The rendered manifests in k8s/.rendered/ can be safely deleted:"
echo "    rm -rf ${RENDERED_DIR}"
echo "======================================================================"
