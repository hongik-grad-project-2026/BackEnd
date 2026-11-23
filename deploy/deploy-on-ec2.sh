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
# Redis(대여 재고 문지기)를 먼저 띄운다. 실패해도 배포는 계속한다 : 백엔드는 Redis 없이 DB로 판단하고,
#  Redis가 나중에 뜨면 주기 재동기화(1분)가 채운다. 배포 성공 조건은 백엔드 헬스체크뿐
#  timeout 120s : --wait-timeout은 준비 대기만 재므로 이미지 다운로드까지 포함한 전체 상한을 둔다 (SSM 제한 600초)
#  --kill-after=5s : 종료 신호(SIGTERM)를 받고도 5초 안에 안 끝나면 강제 종료(SIGKILL)
timeout --kill-after=5s 120s docker compose -f /opt/mulmi/compose.deploy.yml up -d --wait --wait-timeout 60 redis \
  || echo 'WARN: redis is not ready; backend will use DB until the next stock resync' >&2
docker compose -f /opt/mulmi/compose.deploy.yml up -d --wait --wait-timeout 180 backend
curl --fail --silent --show-error http://127.0.0.1/api/health
