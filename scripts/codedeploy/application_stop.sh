#!/usr/bin/env bash
set -euo pipefail

APP_DIR="/home/ubuntu/app"

if [ ! -f "${APP_DIR}/docker-compose.prod.yaml" ]; then
  exit 0
fi

cd "${APP_DIR}"
docker compose -f docker-compose.prod.yaml down --remove-orphans || true
