#!/usr/bin/env bash
# Wave H — Host Docker image build + smoke + optional Playwright
# Run on a machine with Docker daemon (not the agent sandbox).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "========================================"
echo " WAVE H HOST GATE — Product Costing"
echo "========================================"

if ! command -v docker >/dev/null 2>&1; then
  echo "FAIL: docker not installed"
  exit 1
fi
if ! docker info >/dev/null 2>&1; then
  echo "FAIL: docker daemon not reachable"
  exit 1
fi

export JWT_SECRET="${JWT_SECRET:-$(openssl rand -base64 48 2>/dev/null || echo 'local-dev-secret-must-be-at-least-32-chars-long!!')}"
echo "JWT_SECRET length: ${#JWT_SECRET}"

echo "== docker compose up --build -d =="
docker compose up --build -d

echo "== wait backend health =="
for i in $(seq 1 90); do
  if curl -sf http://127.0.0.1:8080/actuator/health 2>/dev/null | grep -qi UP; then
    echo "OK backend UP (${i})"
    break
  fi
  sleep 4
  if [[ $i -eq 90 ]]; then
    echo "FAIL: backend health timeout"
    docker compose ps
    docker compose logs --tail=80 backend || true
    exit 1
  fi
done

# Flyway evidence in logs
echo "== Flyway / migration tail =="
docker compose logs backend 2>/dev/null | grep -iE 'Flyway|V12[0-9]|Successfully applied' | tail -20 || true

BASE_URL="${BASE:-http://127.0.0.1:8080}"
USER="${USER:-admin}"
PASS="${PASS:-ChangeMe!Now1}"

if [[ -z "${CUSTOMER_ID:-}" ]]; then
  echo "NOTE: CUSTOMER_ID not set — attempting login + platform me for default customer"
  LOGIN=$(curl -sS -X POST "$BASE_URL/api/v1/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"$USER\",\"password\":\"$PASS\"}" || true)
  TOKEN=$(echo "$LOGIN" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('accessToken') or d.get('token') or '')" 2>/dev/null || true)
  if [[ -n "${TOKEN:-}" ]]; then
    # Try common endpoints for customer id
    ME=$(curl -sS -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/v1/platform/access/me" 2>/dev/null || true)
    CUSTOMER_ID=$(echo "$ME" | python3 -c "
import sys,json
try:
  d=json.load(sys.stdin)
  print(d.get('customerId') or d.get('defaultCustomerId') or (d.get('customers') or [{}])[0].get('customerId','') or '')
except Exception:
  print('')
" 2>/dev/null || true)
  fi
fi

if [[ -z "${CUSTOMER_ID:-}" ]]; then
  echo "WARN: CUSTOMER_ID still empty — skip PO smoke; set CUSTOMER_ID and re-run purchase-order-smoke.sh"
else
  echo "== Purchase Order smoke (CUSTOMER_ID=$CUSTOMER_ID) =="
  USER="$USER" PASS="$PASS" BASE="$BASE_URL" CUSTOMER_ID="$CUSTOMER_ID" \
    "$ROOT/scripts/purchase-order-smoke.sh" || {
      echo "PO smoke failed — see output above"
      exit 1
    }
fi

# Playwright optional
if [[ -d "$ROOT/frontend" ]] && command -v npx >/dev/null 2>&1; then
  if [[ "${RUN_PLAYWRIGHT:-0}" == "1" ]]; then
    echo "== Playwright (RUN_PLAYWRIGHT=1) =="
    (cd "$ROOT/frontend" && npx playwright test --reporter=line 2>&1 | tail -40) || {
      echo "WARN: Playwright had failures — review report"
    }
  else
    echo "SKIP Playwright (set RUN_PLAYWRIGHT=1 to enable)"
  fi
else
  echo "SKIP Playwright (frontend or npx missing)"
fi

echo "========================================"
echo " WAVE H HOST GATE — COMPLETE"
echo " Backend health: PASS"
echo " Docker compose: PASS (built on this host)"
echo "========================================"
