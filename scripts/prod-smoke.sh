#!/usr/bin/env bash
# Post-deploy smoke checks
# Usage: BASE=https://api.example.com USER=admin PASS=... ./scripts/prod-smoke.sh
set -euo pipefail
BASE="${BASE:-http://127.0.0.1:8080}"
USER="${USER:-admin}"
PASS="${PASS:-ChangeMe!Now1}"

echo "Health..."
curl -sf "$BASE/actuator/health" | tee /tmp/pcs-health.json
grep -q '"status":"UP"' /tmp/pcs-health.json

echo "Login..."
TOKEN=$(curl -sf -X POST "$BASE/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"$USER\",\"password\":\"$PASS\"}" | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")
test -n "$TOKEN"

echo "Modules..."
curl -sf -H "Authorization: Bearer $TOKEN" "$BASE/api/v1/platform/modules" | head -c 200
echo

echo "ACCESS-07 (expect 403)..."
CODE=$(curl -s -o /tmp/pcs-a07.json -w "%{http_code}" \
  -H "Authorization: Bearer $TOKEN" \
  "$BASE/api/v1/vendor-selection/suppliers/c1000000-0000-4000-8000-000000000001/360")
test "$CODE" = "403"
echo "OK smoke complete"
