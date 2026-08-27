#!/usr/bin/env bash
# Wave D host smoke: migrate implied by backend start; create → submit → decide → issue
# Prerequisites:
#   - docker compose up (MySQL + backend healthy) OR live backend with Flyway through V127
#   - PURCHASE module entitled for CUSTOMER_ID
# Usage:
#   export JWT_SECRET=...   # if starting compose
#   BASE=http://127.0.0.1:8080 USER=admin PASS='ChangeMe!Now1' CUSTOMER_ID=<uuid> \
#     ./scripts/purchase-order-smoke.sh
set -euo pipefail

BASE="${BASE:-http://127.0.0.1:8080}"
USER="${USER:-admin}"
PASS="${PASS:-ChangeMe!Now1}"
CUSTOMER_ID="${CUSTOMER_ID:-}"
SUPPLIER_ID="${SUPPLIER_ID:-00000000-0000-4000-8000-000000000010}"

echo "== Purchase Order smoke (Wave D) against $BASE =="

if [[ -z "$CUSTOMER_ID" ]]; then
  echo "FAIL: CUSTOMER_ID required (PURCHASE-entitled tenant)"
  exit 1
fi

LOGIN=$(curl -sS -X POST "$BASE/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"$USER\",\"password\":\"$PASS\"}")
TOKEN=$(echo "$LOGIN" | python3 -c "import sys,json; print(json.load(sys.stdin).get('accessToken') or json.load(open('/dev/stdin')).get('accessToken',''))" 2>/dev/null || true)
# robust token extract
TOKEN=$(echo "$LOGIN" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('accessToken') or d.get('token') or '')")
if [[ -z "${TOKEN:-}" ]]; then
  echo "FAIL: login"
  echo "$LOGIN" | head -c 400
  exit 1
fi
echo "OK login"

AUTH=( -H "Authorization: Bearer $TOKEN" -H "X-Customer-Id: $CUSTOMER_ID" -H "Content-Type: application/json" )

# Health (optional)
HC=$(curl -sS -o /dev/null -w '%{http_code}' "$BASE/actuator/health" || echo 000)
echo "Health HTTP $HC"

# Module gate: without PURCHASE should be 403 — we only assert entitled path works
CREATE_BODY=$(python3 - << PY
import json
print(json.dumps({
  "supplierId": "$SUPPLIER_ID",
  "plantCode": "P01",
  "currencyCode": "USD",
  "paymentTerms": "NET30",
  "incoterms": "FOB",
  "lines": [{
    "materialCode": "SMOKE-MAT-1",
    "materialDescription": "PO smoke line",
    "quantity": 5,
    "uom": "EA",
    "unitPrice": 12.5,
    "currencyCode": "USD"
  }]
}))
PY
)

CODE=$(curl -sS -o /tmp/po-create.json -w '%{http_code}' \
  "${AUTH[@]}" -X POST "$BASE/api/v1/purchase-orders" -d "$CREATE_BODY")
echo "Create HTTP $CODE"
if [[ "$CODE" != "201" && "$CODE" != "200" ]]; then
  echo "FAIL create"
  cat /tmp/po-create.json
  echo
  echo "HINT: 403 = PURCHASE not entitled for CUSTOMER_ID; ensure Flyway V122+ and entitlement"
  exit 1
fi
PO_ID=$(python3 -c "import json; print(json.load(open('/tmp/po-create.json')).get('purchaseOrderId',''))")
STATUS=$(python3 -c "import json; print(json.load(open('/tmp/po-create.json')).get('status',''))")
echo "OK create id=$PO_ID status=$STATUS"
[[ "$STATUS" == "DRAFT" ]] || { echo "FAIL expected DRAFT"; exit 1; }

CODE=$(curl -sS -o /tmp/po-submit.json -w '%{http_code}' \
  "${AUTH[@]}" -X POST "$BASE/api/v1/purchase-orders/$PO_ID/submit")
echo "Submit HTTP $CODE"
STATUS=$(python3 -c "import json; print(json.load(open('/tmp/po-submit.json')).get('status',''))" 2>/dev/null || echo '')
[[ "$CODE" == "200" && "$STATUS" == "APPROVAL" ]] || { echo "FAIL submit"; cat /tmp/po-submit.json; exit 1; }
echo "OK submit → APPROVAL"

CODE=$(curl -sS -o /tmp/po-decide.json -w '%{http_code}' \
  "${AUTH[@]}" -X POST "$BASE/api/v1/purchase-orders/$PO_ID/decide" \
  -d '{"decisionCode":"APPROVE","decisionReason":"Host smoke approve"}')
STATUS=$(python3 -c "import json; print(json.load(open('/tmp/po-decide.json')).get('status',''))" 2>/dev/null || echo '')
echo "Decide HTTP $CODE status=$STATUS"
[[ "$CODE" == "200" && "$STATUS" == "APPROVED" ]] || { echo "FAIL decide"; cat /tmp/po-decide.json; exit 1; }
echo "OK decide → APPROVED"

CODE=$(curl -sS -o /tmp/po-issue.json -w '%{http_code}' \
  "${AUTH[@]}" -X POST "$BASE/api/v1/purchase-orders/$PO_ID/issue" \
  -d '{"externalPoReference":"SMOKE-ERP-PO-1"}')
STATUS=$(python3 -c "import json; print(json.load(open('/tmp/po-issue.json')).get('status',''))" 2>/dev/null || echo '')
EXT=$(python3 -c "import json; print(json.load(open('/tmp/po-issue.json')).get('externalPoReference',''))" 2>/dev/null || echo '')
echo "Issue HTTP $CODE status=$STATUS ref=$EXT"
[[ "$CODE" == "200" && "$STATUS" == "ISSUED" ]] || { echo "FAIL issue"; cat /tmp/po-issue.json; exit 1; }
echo "OK issue → ISSUED"

# Negative: create without auth
CODE401=$(curl -sS -o /dev/null -w '%{http_code}' -X POST "$BASE/api/v1/purchase-orders" -H 'Content-Type: application/json' -d "$CREATE_BODY" || echo 000)
[[ "$CODE401" == "401" || "$CODE401" == "403" ]] || { echo "FAIL expected 401/403 without auth got $CODE401"; exit 1; }
echo "OK anonymous denied ($CODE401)"

echo "PASS purchase-order-smoke (create→submit→decide→issue) on PURCHASE tenant $CUSTOMER_ID"
