#!/usr/bin/env bash
# Full deployment: Terraform (EKS + RDS) → ECR image push → kubectl apply.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
TF_DIR="${PROJECT_ROOT}/infra/terraform"
K8S_DIR="${PROJECT_ROOT}/infra/k8s"
RENDERED_DIR="${K8S_DIR}/.rendered"

echo "======================================================================"
echo "  beer-catalogue-api  —  Deploy to AWS (EKS + RDS)"
echo "======================================================================"
echo

echo ">>> Checking prerequisites..."
for cmd in terraform aws kubectl docker; do
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

if [[ -z "${DB_PASSWORD:-}" ]]; then
  echo
  echo ">>> DB_PASSWORD is not set in the environment."
  read -r -s -p "    Enter RDS master password (input hidden): " DB_PASSWORD
  echo
fi
[[ -z "${DB_PASSWORD:-}" ]] && { echo "ERROR: DB_PASSWORD must not be empty." >&2; exit 1; }

DB_USERNAME="${DB_USERNAME:-beeradmin}"

echo
echo ">>> [1/6] Terraform: init + validate + apply (15–20 min)..."
cd "$TF_DIR"
terraform init -input=false
terraform validate
terraform apply -auto-approve -var="db_password=${DB_PASSWORD}"

echo ">>> [1/6] Capturing Terraform outputs..."
REGION=$(terraform output -raw region)
CLUSTER_NAME=$(terraform output -raw cluster_name)
RDS_ENDPOINT=$(terraform output -raw rds_endpoint)
echo "    region:       ${REGION}"
echo "    cluster_name: ${CLUSTER_NAME}"
echo "    rds_endpoint: ${RDS_ENDPOINT}"

echo
echo ">>> [2/6] Configuring kubectl for cluster '${CLUSTER_NAME}'..."
cd "$PROJECT_ROOT"
aws eks update-kubeconfig --region "$REGION" --name "$CLUSTER_NAME"

echo
echo ">>> [3/6] Building and pushing Docker image to ECR..."
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
ECR_REPO="beer-catalogue-api"
IMAGE_URI="${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com/${ECR_REPO}:latest"
echo "    Image URI: ${IMAGE_URI}"

if ! aws ecr describe-repositories --repository-names "$ECR_REPO" --region "$REGION" &>/dev/null; then
  echo "    Creating ECR repository '${ECR_REPO}'..."
  aws ecr create-repository --repository-name "$ECR_REPO" --region "$REGION" --output text &>/dev/null
fi

aws ecr get-login-password --region "$REGION" \
  | docker login --username AWS --password-stdin \
      "${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com"

docker build -t "${IMAGE_URI}" "${PROJECT_ROOT}"
docker push "${IMAGE_URI}"

echo
echo ">>> [4/6] Rendering Kubernetes manifests into ${RENDERED_DIR}/..."
rm -rf "${RENDERED_DIR}"
mkdir -p "${RENDERED_DIR}"

# configmap.yaml: substitute DB_HOST placeholder with real RDS endpoint
sed "s|<RDS endpoint from: terraform output rds_endpoint>|${RDS_ENDPOINT}|" \
  "${K8S_DIR}/configmap.yaml" > "${RENDERED_DIR}/configmap.yaml"

# secret.yaml: substitute base64-encoded credential placeholders
DB_USER_B64=$(printf '%s' "${DB_USERNAME}" | base64 | tr -d '\n')
DB_PASS_B64=$(printf '%s' "${DB_PASSWORD}" | base64 | tr -d '\n')
sed \
  -e "s|DB_USER:.*|DB_USER: ${DB_USER_B64}|" \
  -e "s|DB_PASSWORD:.*|DB_PASSWORD: ${DB_PASS_B64}|" \
  "${K8S_DIR}/secret.yaml" > "${RENDERED_DIR}/secret.yaml"

# deployment.yaml: substitute image placeholder with real ECR URI
sed "s|<your-registry>/beer-catalogue-api:latest|${IMAGE_URI}|" \
  "${K8S_DIR}/deployment.yaml" > "${RENDERED_DIR}/deployment.yaml"

# service.yaml: no substitutions needed
cp "${K8S_DIR}/service.yaml" "${RENDERED_DIR}/service.yaml"

echo "    Rendered: configmap.yaml, secret.yaml, deployment.yaml, service.yaml"

echo
echo ">>> [5/6] Applying Kubernetes manifests and waiting for rollout..."
kubectl apply -f "${RENDERED_DIR}/"
kubectl rollout status deployment/beer-catalogue --timeout=300s

echo
echo ">>> [6/6] Waiting for LoadBalancer hostname (up to 2 min)..."
ELB_HOSTNAME=""
for i in $(seq 1 12); do
  ELB_HOSTNAME=$(kubectl get svc beer-catalogue \
    -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' 2>/dev/null || true)
  [[ -n "$ELB_HOSTNAME" ]] && break
  echo "    ...attempt ${i}/12, retrying in 10s"
  sleep 10
done

echo
echo "======================================================================"
echo "  Deployment complete!"
echo "======================================================================"
if [[ -n "$ELB_HOSTNAME" ]]; then
  echo
  echo "  LoadBalancer: ${ELB_HOSTNAME}"
  echo
  echo "  Sample requests (ELB may take 1–2 min to become DNS-resolvable):"
  echo "    curl http://${ELB_HOSTNAME}/api/beers"
  echo "    curl http://${ELB_HOSTNAME}/swagger-ui/index.html"
else
  echo
  echo "  LoadBalancer hostname not yet assigned. Check later with:"
  echo "    kubectl get svc beer-catalogue"
fi
echo
echo "  REMINDER: run ./scripts/destroy.sh when done."
echo "======================================================================"
