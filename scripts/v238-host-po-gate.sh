#!/usr/bin/env bash
# Host gate: Docker image build (if docker present) + PO smoke after V127 migrate
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "== V238 host PO gate =="

if command -v docker >/dev/null 2>&1; then
  export JWT_SECRET="${JWT_SECRET:-$(openssl rand -base64 48 2>/dev/null || echo 'local-dev-secret-must-be-at-least-32-chars!!')}"
  echo "Docker present — compose up --build"
  docker compose up --build -d
  echo "Waiting for backend health..."
  for i in $(seq 1 60); do
    if curl -sf http://127.0.0.1:8080/actuator/health | grep -q UP; then
      echo "Backend UP"
      break
    fi
    sleep 5
    if [[ $i -eq 60 ]]; then echo "FAIL backend health timeout"; docker compose ps; exit 1; fi
  done
else
  echo "NOTE: docker not installed on this machine — skip image build; assume BASE is already up"
fi

BASE="${BASE:-http://127.0.0.1:8080}"
# Discover DEFAULT customer id if not set (platform API) — caller should set CUSTOMER_ID
if [[ -z "${CUSTOMER_ID:-}" ]]; then
  echo "Set CUSTOMER_ID to a PURCHASE-entitled customer UUID (Flyway V122 enables PURCHASE on DEFAULT)."
  echo "Example after login: GET /api/v1/platform/access/me or platform customers list"
  exit 2
fi

USER="${USER:-admin}" PASS="${PASS:-ChangeMe!Now1}" BASE="$BASE" CUSTOMER_ID="$CUSTOMER_ID" \
  "$ROOT/scripts/purchase-order-smoke.sh"
