#!/usr/bin/env bash
set -euo pipefail

APP_DIR="/home/ubuntu/app"

cd "${APP_DIR}"

set -a
source .env
set +a

for attempt in $(seq 1 30); do
  if curl -fsS "http://127.0.0.1:${APP_PORT:-8080}/actuator/health" | grep -q '"status":"UP"'; then
    exit 0
  fi
  sleep 10
done

docker compose -f docker-compose.prod.yaml logs --tail=200 app
exit 1
