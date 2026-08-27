#!/usr/bin/env bash
# Host smoke for Customer Helping AI Agent (read-only).
# Requires backend up on BASE (default http://localhost:8080).
# CUSTOMER_ID with PRODUCT_COSTING enabled is required for successful chat (ACCESS-07).
set -euo pipefail
BASE="${BASE:-http://localhost:8080}"
USER="${USER:-admin}"
PASS="${PASS:-ChangeMe!Now1}"
CUSTOMER_ID="${CUSTOMER_ID:-}"

echo "== Helping Agent smoke against $BASE =="
LOGIN=$(curl -sS -X POST "$BASE/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"$USER\",\"password\":\"$PASS\"}")
TOKEN=$(echo "$LOGIN" | python3 -c "import sys,json; print(json.load(sys.stdin).get('accessToken',''))")
if [[ -z "$TOKEN" ]]; then
  echo "FAIL: login"
  exit 1
fi
echo "OK login"

# Unauthenticated must fail
CODE=$(curl -sS -o /dev/null -w '%{http_code}' -X POST "$BASE/api/v1/helping-agent/chat" \
  -H 'Content-Type: application/json' -d '{"question":"Explain BOM"}' || echo 000)
if [[ "$CODE" != "401" && "$CODE" != "403" ]]; then
  echo "FAIL: expected 401/403 for anonymous chat, got $CODE"
  exit 1
fi
echo "OK anonymous denied ($CODE)"

# Without customer header → ACCESS-07
CODE2=$(curl -sS -o /dev/null -w '%{http_code}' -X POST "$BASE/api/v1/helping-agent/chat" \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"question":"Explain dashboard","context":{"currentPage":"dashboard"}}' || echo 000)
if [[ "$CODE2" != "403" ]]; then
  echo "FAIL: expected 403 ACCESS-07 without X-Customer-Id, got $CODE2"
  exit 1
fi
echo "OK ACCESS-07 without X-Customer-Id ($CODE2)"

if [[ -z "$CUSTOMER_ID" ]]; then
  # Discover DEFAULT customer if platform admin
  CUSTOMER_ID=$(curl -sS -H "Authorization: Bearer $TOKEN" "$BASE/api/v1/platform/customers" \
    | python3 -c "import sys,json; d=json.load(sys.stdin); print(d[0]['customerId'] if d else '')" 2>/dev/null || true)
fi
if [[ -z "$CUSTOMER_ID" ]]; then
  echo "SKIP authenticated chat: set CUSTOMER_ID to a tenant with PRODUCT_COSTING enabled"
  echo "== Helping Agent smoke PARTIAL PASS =="
  exit 0
fi

RESP=$(curl -sS -w "\n%{http_code}" -X POST "$BASE/api/v1/helping-agent/chat" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Customer-Id: $CUSTOMER_ID" \
  -H 'Content-Type: application/json' \
  -d '{"question":"Explain the dashboard KPIs","context":{"currentPage":"dashboard"},"correlationId":"smoke-ha-1"}')
HTTP=$(echo "$RESP" | tail -n1)
BODY=$(echo "$RESP" | sed '$d')
echo "chat HTTP $HTTP"
if [[ "$HTTP" == "403" ]]; then
  echo "$BODY" | grep -E 'ACCESS-03|MODULE|disabled|PRODUCT_COSTING' && echo "OK module denial path" && exit 0
  echo "FAIL unexpected 403: $BODY"
  exit 1
fi
if [[ "$HTTP" != "200" ]]; then
  echo "FAIL chat status $HTTP body=$BODY"
  exit 1
fi
echo "$BODY" | python3 -c '
import sys, json
body = json.load(sys.stdin)
cls = body.get("classification")
ans = body.get("answer") or ""
corr = body.get("correlationId")
assert cls in {"FACT","CALCULATED","RULE_RESULT","AI_INFERENCE","MISSING_DATA"}, cls
assert len(ans) > 0
assert corr == "smoke-ha-1"
print("OK chat classification=", cls, "answer_len=", len(ans), "sources=", len(body.get("sources") or []))
'
echo "== Helping Agent smoke PASS =="
