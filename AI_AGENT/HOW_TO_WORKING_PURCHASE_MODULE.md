# How to use a working Purchase module (P3-01)

**Package:** MASTER-V231+  
**Module code:** `PURCHASE`  
**UI:** `/purchase-requests`  
**API:** `@RequiresModule("PURCHASE")`

---

## 1. Enable module (once per tenant)

### Fresh DEFAULT tenant
Flyway **V122** enables `PURCHASE` (+ `ORDER_TRACKING`) for customer `DEFAULT`.

### Existing customer (Platform Admin)
1. Sign in as **ADMIN** / **SYSTEM_ADMINISTRATOR**
2. **Platform → Modules / Customers** — enable module **Purchase** for the tenant
3. Sign out and back in as the engineer (refresh entitlements)

Without entitlement, the menu hides **Purchase Requests** and APIs return **403**.

---

## 2. Preconditions for a real purchase request

| Gate | Requirement |
|------|-------------|
| Module | `PURCHASE` enabled for tenant + `X-Customer-Id` |
| Make-vs-Buy | Status **DECIDED**, decision `APPROVE_BUY` or `APPROVE_MIXED` **or** `manualOverrideReason` |
| Supplier | Not `BLOCKED`; qualification lifecycle **APPROVED** |
| Quote | Prefer **ACCEPTED**; must match BOM node + supplier |
| FX | Missing rate → line `NOT_AVAILABLE` (no invented rate) |
| RELEASE | Blocked if any line is `NOT_AVAILABLE` |
| Approve/release | **Human only** — AI never approves |

---

## 3. Happy path (UI)

1. Complete **Make vs Buy** → Compare → human **Decide** (`APPROVE_BUY` / `APPROVE_MIXED`)
2. Open **Purchase Requests**
3. Enter **BOM Node ID** + **Make-vs-Buy Simulation ID** → **Create DRAFT**  
   (or leave MvB empty and set **manual override reason** + lines)
4. **Submit** → human **Approve** (reason required) → **Release** (when all lines AVAILABLE)
5. **Export package** — JSON for CPI/ERP (**does not post to SAP MM**)

---

## 4. API smoke

```bash
export BASE=http://127.0.0.1:8080
export CUSTOMER_ID=<default-customer-uuid>
./scripts/purchase-smoke.sh
```

Full create/submit/decide needs real BOM, DECIDED MvB, APPROVED supplier.

---

## 5. Troubleshooting

| Symptom | Cause | Fix |
|---------|--------|-----|
| No menu item | Module not entitled / not ADMIN | Enable PURCHASE; re-login |
| 403 on API | Missing module or `X-Customer-Id` | Entitlement + header |
| Create fails: not DECIDED | MvB still DRAFT/COMPARED | Decide MvB first |
| Create fails: APPROVED required | Supplier not APPROVED in VS | Complete VS qualification |
| Cannot Release | Line `NOT_AVAILABLE` | Fix price/FX; do not invent rates |
