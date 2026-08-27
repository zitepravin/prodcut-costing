#!/usr/bin/env bash
# V230 Host Gate — Docker + Playwright + live CUSTOMER_ADMIN rate-approve 403
# Run on a machine with Docker, Node 20+, and network access.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "=== V230 Host Gate ==="
echo "Root: $ROOT"

command -v docker >/dev/null || { echo "FAIL: docker not found"; exit 1; }
command -v curl >/dev/null || { echo "FAIL: curl not found"; exit 1; }

export JWT_SECRET="${JWT_SECRET:-$(openssl rand -base64 48)}"

echo "=== docker compose up --build -d ==="
docker compose up --build -d

echo "=== wait for health ==="
for i in $(seq 1 60); do
  if curl -sf http://127.0.0.1:8080/actuator/health | grep -q UP; then
    echo "backend UP"
    break
  fi
  sleep 5
  if [ "$i" -eq 60 ]; then
    echo "FAIL: backend health timeout"
    docker compose logs backend | tail -80
    exit 1
  fi
done

docker compose ps

echo "=== Playwright (against Docker FE :8088) ==="
cd frontend
npm ci
npx playwright install chromium
export PLAYWRIGHT_BASE_URL="${PLAYWRIGHT_BASE_URL:-http://127.0.0.1:8088}"
npm run test:e2e
cd "$ROOT"

echo "=== Live CUSTOMER_ADMIN rate-approve denial (API) ==="
# Expect 401 without token; with CUSTOMER_ADMIN token expect 403 on approve endpoints.
# Replace TOKEN_CUSTOMER_ADMIN after logging in as customer admin.
APPROVE_URLS=(
  "http://127.0.0.1:8080/api/v1/master/materials/any-id/approve"
  "http://127.0.0.1:8080/api/v1/master/machines/any-id/approve"
  "http://127.0.0.1:8080/api/v1/master/labor/any-id/approve"
)
for u in "${APPROVE_URLS[@]}"; do
  code=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$u" -H "Authorization: Bearer ${TOKEN_CUSTOMER_ADMIN:-invalid}")
  echo "POST $u -> HTTP $code (expect 401/403, never 200)"
done

echo "=== V230 Host Gate complete ==="
echo "Review docker compose ps, Playwright output, and HTTP codes above."
echo "Only then update readiness to READY."
