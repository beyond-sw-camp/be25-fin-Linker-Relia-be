#!/usr/bin/env bash
set -euo pipefail

APP_DIR="/home/ubuntu/app"

mkdir -p "${APP_DIR}"
cd "${APP_DIR}"

chmod +x scripts/codedeploy/*.sh

if [ ! -f deployment/release.env ]; then
  echo "deployment/release.env file is required for production deployment." >&2
  exit 1
fi

set -a
source deployment/release.env
set +a

if [ -z "${APP_ENV_SSM_PARAMETER:-}" ]; then
  echo "APP_ENV_SSM_PARAMETER is required." >&2
  exit 1
fi

if [ -z "${GHCR_USERNAME_SSM_PARAMETER:-}" ]; then
  echo "GHCR_USERNAME_SSM_PARAMETER is required." >&2
  exit 1
fi

if [ -z "${GHCR_TOKEN_SSM_PARAMETER:-}" ]; then
  echo "GHCR_TOKEN_SSM_PARAMETER is required." >&2
  exit 1
fi

TMP_JSON="$(mktemp)"
aws ssm get-parameter \
  --region "${AWS_REGION}" \
  --with-decryption \
  --name "${APP_ENV_SSM_PARAMETER}" \
  --output json > "${TMP_JSON}"

python3 - "${TMP_JSON}" "${APP_DIR}/.env" <<'PY'
import json
import pathlib
import sys

payload_path = pathlib.Path(sys.argv[1])
target_path = pathlib.Path(sys.argv[2])
data = json.loads(payload_path.read_text(encoding="utf-8"))
target_path.write_text(data["Parameter"]["Value"] + "\n", encoding="utf-8")
PY

rm -f "${TMP_JSON}"
