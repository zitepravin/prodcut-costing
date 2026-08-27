#!/usr/bin/env bash
# End-to-end P2P operational cycle (Create PO center).
# Host with running backend. Does not post to SAP / GR / IR.
set -euo pipefail
BASE="${BASE:-http://127.0.0.1:8080}"
USER="${USER:-admin}"
PASS="${PASS:-ChangeMe!Now1}"
CUSTOMER_ID="${CUSTOMER_ID:-}"
SUPPLIER_ID="${SUPPLIER_ID:-00000000-0000-4000-8000-000000000010}"

echo "== P2P cycle smoke (Create PO → Close) against $BASE =="

if [[ -z "$CUSTOMER_ID" ]]; then
  echo "FAIL: set CUSTOMER_ID to a PURCHASE-entitled tenant"
  exit 1
fi

LOGIN=$(curl -sS -X POST "$BASE/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"$USER\",\"password\":\"$PASS\"}")
TOKEN=$(echo "$LOGIN" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('accessToken') or d.get('token') or '')")
[[ -n "$TOKEN" ]] || { echo "FAIL login"; echo "$LOGIN"; exit 1; }
echo "OK login"

AUTH=( -H "Authorization: Bearer $TOKEN" -H "X-Customer-Id: $CUSTOMER_ID" -H "Content-Type: application/json" )

CREATE=$(python3 - << PY
import json
print(json.dumps({
  "supplierId": "$SUPPLIER_ID",
  "plantCode": "P01",
  "currencyCode": "USD",
  "paymentTerms": "NET30",
  "lines": [{
    "materialCode": "P2P-MAT-1",
    "materialDescription": "P2P cycle line",
    "quantity": 2,
    "uom": "EA",
    "unitPrice": 25.00,
    "currencyCode": "USD"
  }]
}))
PY
)

code=$(curl -sS -o /tmp/p2p-create.json -w '%{http_code}' \
  "${AUTH[@]}" -X POST "$BASE/api/v1/purchase-orders" -d "$CREATE")
echo "1 Create PO HTTP $code"
[[ "$code" == "201" || "$code" == "200" ]] || { cat /tmp/p2p-create.json; exit 1; }
PO_ID=$(python3 -c "import json; print(json.load(open('/tmp/p2p-create.json'))['purchaseOrderId'])")
ST=$(python3 -c "import json; print(json.load(open('/tmp/p2p-create.json'))['status'])")
[[ "$ST" == "DRAFT" ]] || { echo "FAIL expected DRAFT got $ST"; exit 1; }
echo "   PO $PO_ID DRAFT"

step() {
  local name="$1" method="$2" url="$3" body="${4:-}"
  if [[ -n "$body" ]]; then
    code=$(curl -sS -o /tmp/p2p-step.json -w '%{http_code}' "${AUTH[@]}" -X "$method" "$url" -d "$body")
  else
    code=$(curl -sS -o /tmp/p2p-step.json -w '%{http_code}' "${AUTH[@]}" -X "$method" "$url")
  fi
  ST=$(python3 -c "import json; print(json.load(open('/tmp/p2p-step.json')).get('status',''))" 2>/dev/null || echo '')
  echo "$name HTTP $code status=$ST"
  [[ "$code" == "200" ]] || { cat /tmp/p2p-step.json; exit 1; }
}

step "2 Submit" POST "$BASE/api/v1/purchase-orders/$PO_ID/submit"
[[ "$ST" == "APPROVAL" ]] || exit 1

step "3 Decide APPROVE" POST "$BASE/api/v1/purchase-orders/$PO_ID/decide" \
  '{"decisionCode":"APPROVE","decisionReason":"P2P cycle approve"}'
[[ "$ST" == "APPROVED" ]] || exit 1

step "4 Issue" POST "$BASE/api/v1/purchase-orders/$PO_ID/issue" \
  '{"externalPoReference":"P2P-EXT-1"}'
[[ "$ST" == "ISSUED" ]] || exit 1

step "5 Partial receive" POST "$BASE/api/v1/purchase-orders/$PO_ID/close" \
  '{"partial":true,"reason":"P2P partial"}'
[[ "$ST" == "PARTIALLY_RECEIVED" ]] || exit 1

step "6 Close" POST "$BASE/api/v1/purchase-orders/$PO_ID/close" \
  '{"partial":false,"reason":"P2P complete"}'
[[ "$ST" == "CLOSED" ]] || exit 1

echo "PASS P2P cycle: DRAFT → APPROVAL → APPROVED → ISSUED → PARTIALLY_RECEIVED → CLOSED"
echo "NOTE: PR→RFQ→Create-PO-from-PR leg needs program/BOM seed; use UI or seed then create-purchase-order."
