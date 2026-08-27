#!/usr/bin/env bash
# Purchase module smoke (P3-01). Does not create business data or approve PRs.
# Usage: BASE=http://127.0.0.1:8080 USER=admin PASS=... CUSTOMER_ID=<uuid> ./scripts/purchase-smoke.sh
set -euo pipefail
BASE="${BASE:-http://127.0.0.1:8080}"
USER="${USER:-admin}"
PASS="${PASS:-ChangeMe!Now1}"
CUSTOMER_ID="${CUSTOMER_ID:-}"

echo "== Purchase smoke against $BASE =="

LOGIN=$(curl -sS -X POST "$BASE/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"$USER\",\"password\":\"$PASS\"}")
TOKEN=$(echo "$LOGIN" | python3 -c "import sys,json; print(json.load(sys.stdin).get('accessToken',''))" 2>/dev/null || true)
if [[ -z "${TOKEN:-}" ]]; then
  echo "FAIL: login"
  exit 1
fi
echo "OK login"

AUTH=( -H "Authorization: Bearer $TOKEN" )
if [[ -n "$CUSTOMER_ID" ]]; then
  AUTH+=( -H "X-Customer-Id: $CUSTOMER_ID" )
fi

# Unauthenticated list must fail
CODE=$(curl -sS -o /dev/null -w '%{http_code}' \
  "$BASE/api/v1/programs/00000000-0000-4000-8000-000000000001/purchase-requests" || echo 000)
if [[ "$CODE" != "401" && "$CODE" != "403" ]]; then
  echo "FAIL: expected 401/403 without auth, got $CODE"
  exit 1
fi
echo "OK anonymous denied ($CODE)"

# Authenticated call — 200/403/404 acceptable depending on entitlement + program existence
CODE2=$(curl -sS -o /tmp/pcs-pr-list.json -w '%{http_code}' \
  "${AUTH[@]}" \
  "$BASE/api/v1/programs/00000000-0000-4000-8000-000000000001/purchase-requests" || echo 000)
echo "Authenticated list HTTP $CODE2 (200=entitled+data path, 403=module/tenant gate, 404=program missing)"
if [[ "$CODE2" != "200" && "$CODE2" != "403" && "$CODE2" != "404" ]]; then
  echo "FAIL: unexpected status $CODE2"
  cat /tmp/pcs-pr-list.json 2>/dev/null || true
  exit 1
fi
echo "OK purchase API reachable under policy ($CODE2)"

# Export endpoint on random id should not be 500 (404/403 expected)
RID="00000000-0000-4000-8000-000000000099"
CODE3=$(curl -sS -o /tmp/pcs-pr-exp.json -w '%{http_code}' \
  "${AUTH[@]}" \
  "$BASE/api/v1/purchase-requests/$RID/export-package" || echo 000)
if [[ "$CODE3" == "500" ]]; then
  echo "FAIL: export-package returned 500"
  cat /tmp/pcs-pr-exp.json 2>/dev/null || true
  exit 1
fi
echo "OK export-package fail-closed ($CODE3, not 500)"

echo "OK purchase smoke complete"
echo "NOTE: Full create/submit/decide/release requires DECIDED MvB + APPROVED supplier — run manual §6."
