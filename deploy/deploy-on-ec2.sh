#!/usr/bin/env bash
set -euo pipefail

: "${AWS_REGION:?AWS_REGION must be set}"
: "${AWS_ACCOUNT_ID:?AWS_ACCOUNT_ID must be set}"
: "${ECR_REPOSITORY:?ECR_REPOSITORY must be set}"
: "${IMAGE_TAG:?IMAGE_TAG must be set}"

test -s /opt/mulmi/backend.env || {
  echo 'Missing /opt/mulmi/backend.env' >&2
  exit 1
}

registry="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
export IMAGE_URI="${registry}/${ECR_REPOSITORY}:${IMAGE_TAG}"

aws ecr get-login-password --region "${AWS_REGION}" \
  | docker login --username AWS --password-stdin "${registry}"
docker compose -f /opt/mulmi/compose.deploy.yml pull backend
docker compose -f /opt/mulmi/compose.deploy.yml up -d --wait --wait-timeout 180 backend
curl --fail --silent --show-error http://127.0.0.1/api/health
