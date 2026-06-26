#!/usr/bin/env bash
set -euo pipefail

APP_DIR="/home/ubuntu/app"

cd "${APP_DIR}"

set -a
source deployment/release.env
set +a

GHCR_USERNAME="$(aws ssm get-parameter --region "${AWS_REGION}" --with-decryption --name "${GHCR_USERNAME_SSM_PARAMETER}" --query 'Parameter.Value' --output text)"
GHCR_TOKEN="$(aws ssm get-parameter --region "${AWS_REGION}" --with-decryption --name "${GHCR_TOKEN_SSM_PARAMETER}" --query 'Parameter.Value' --output text)"

printf '%s' "${GHCR_TOKEN}" | docker login ghcr.io -u "${GHCR_USERNAME}" --password-stdin

docker compose -f docker-compose.prod.yaml pull app redis
docker compose -f docker-compose.prod.yaml up -d redis app
