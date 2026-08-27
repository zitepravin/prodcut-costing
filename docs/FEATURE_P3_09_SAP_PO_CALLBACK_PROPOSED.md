# FEATURE P3-09 — CPI Callback: External SAP PO Number (PROPOSED REQUIREMENT)

**Document type:** PROPOSED REQUIREMENT  
**Gap ID:** P3-09  
**Date:** 2026-08-15  
**Status:** **DESIGN APPROVED** — §11 locked; **P3-09a IMPLEMENTED** in MASTER-V187  
**Authorization:** `AUTHORIZE P3-xx DESIGN ONLY` (scoped as CPI callback for external SAP PO number)  
**Baseline package:** MASTER-V185 (Flyway V102)

---

## 1. Business purpose

After a human **RELEASES** a Purchase Request in this platform, the customer’s **CPI** may create the real SAP MM purchase order. The organization then needs this platform to **store the SAP PO number** (and related metadata) as a **read-only mirror** so users can see “what was created in ERP” without this app posting to SAP.

```text
RELEASED Purchase Request
        ↓
Export package (JSON) — already exists
        ↓
Customer CPI creates SAP PO (ME21N / BAPI — outside our runtime)
        ↓
CPI CALLBACK → store external SAP PO number on our PR
        ↓
UI shows SAP PO reference (audit trail)
```

---

## 2. Explicit non-goals (frozen)

| Non-goal | Reason |
|----------|--------|
| App calls SAP RFC / OData to create PO | Policy — no direct SAP MM write |
| App triggers ME21N | CPI/ERP owns execution |
| Auto-release PR when SAP PO exists | Human release remains mandatory |
| Auto-approve supplier because PO exists | Human VS approval remains mandatory |
| Full MM change history / PO item mirror (v1) | Scope control — number + status first |
| Invent PO number if CPI fails | Missing → leave null / NOT_LINKED |
| AI invents or confirms PO numbers | Help-only |

---

## 3. Preconditions

1. Purchase Request status = **RELEASED** (or allow callback only after RELEASED — recommended).  
2. Module entitlement `PURCHASE` enabled.  
3. Caller is authenticated **CPI/service** principal **or** admin role with tenant scope (open: §11).  
4. Payload identifies the PR: `purchaseRequestId` **or** stable correlation id from export package.  
5. Tenant isolation enforced on every write.

---

## 4. Proposed domain model

### 4.1 Existing fields (already present)

| Field | Table | Notes |
|-------|--------|-------|
| `external_po_reference` | `purchase_request` | VARCHAR(100) — currently settable on human Release |
| `external_po_reference` | `purchase_order_tracking` | Optional copy on tracking rows |

### 4.2 Proposed additions (if implementation authorized)

| Field / entity | Purpose |
|----------------|---------|
| `external_po_system` | e.g. `SAP_MM` |
| `external_po_number` | SAP PO number (may equal or refine `external_po_reference`) |
| `external_po_item` | Optional line-level later (v1 header only) |
| `po_link_status` | `NOT_LINKED` / `LINKED` / `FAILED` / `SUPERSEDED` |
| `po_linked_at` | Timestamp of successful callback |
| `po_linked_source` | `CPI_CALLBACK` / `HUMAN_RELEASE` / `MANUAL_ADMIN` |
| `po_link_event_id` | CPI idempotency key |
| `po_link_payload_ref` | Optional correlation / message id |
| Audit log | Create/update on every callback |

**Minimal v1 option:** only formalize API + audit around existing `external_po_reference` + add `po_link_status` / `po_linked_at` / `po_link_event_id` via Flyway.

### 4.3 Optional event table (recommended for audit)

`purchase_po_link_event`  
- event_id, purchase_request_id, external_po_number, source_event_id (unique), status, received_at, raw_note (no secrets)

Idempotent upsert on `(source_system, source_event_id)`.

---

## 5. Proposed API (design only)

| Method | Path | Purpose |
|--------|------|---------|
| **POST** | `/api/v1/erp/cpi/purchase-requests/{id}/sap-po-link` | CPI callback: attach SAP PO number |
| **POST** | `/api/v1/erp/cpi/purchase-po-link` | Alternate: body carries `purchaseRequestId` + PO fields |
| **GET** | `/api/v1/purchase-requests/{id}` | Response already exposes `externalPoReference`; extend with link status |

### 5.1 Callback request (conceptual)

```json
{
  "purchaseRequestId": "uuid",
  "externalPoNumber": "4500123456",
  "externalPoSystem": "SAP_MM",
  "companyCode": "1000",
  "sourceEventId": "cpi-msg-987",
  "postedAt": "2026-08-15T12:00:00Z",
  "notes": "Created via BAPI_PO_CREATE1"
}
```

### 5.2 Rules

| Rule | Behavior |
|------|----------|
| PR not found | 404 |
| PR not RELEASED | 422 business rule (recommended) |
| Duplicate `sourceEventId` | Idempotent success (same payload) or conflict if different PO number |
| Blank PO number | Reject — do not store empty as success |
| Wrong tenant | 403 |
| Does **not** change PR status backward | Status stays RELEASED |
| Does **not** call SAP | Confirmation only |

---

## 6. Security

| Topic | Proposal |
|-------|----------|
| Auth | JWT service account for CPI **or** mTLS at gateway (customer infra) |
| Scope | `@RequiresModule("PURCHASE")` + program/tenant access |
| Roles | `SYSTEM_ADMINISTRATOR` / dedicated `CPI_INTEGRATION` role (open §11) |
| Secrets | No SAP passwords stored in our DB |
| Audit | Always record actor, event id, old/new PO reference |

---

## 7. UI

| Surface | Behavior |
|---------|----------|
| Purchase Request detail | Show **SAP PO number** + link status + linked-at when present |
| If `NOT_LINKED` | Display **DATA NOT AVAILABLE** / “Awaiting CPI callback” — not zero, not fake number |
| Manual admin override | Optional later: human sets reference with reason (already partially possible at Release) |

---

## 8. Interaction with P3-05 / P3-06

| Module | Interaction |
|--------|-------------|
| Order tracking | May copy `externalPoReference` when ASN arrives (already does optional fill) |
| Delivery performance | Unchanged; still needs ASN + need-by |
| Export package | May include `poLinkStatus` once implemented |

---

## 9. Phased implementation (if later authorized)

| Phase | Scope |
|-------|--------|
| **P3-09a** | Callback API + persist on RELEASED PR + audit + idempotency |
| **P3-09b** | FE display of link status |
| **P3-09c** | Event history table + admin resync note |
| **Out** | Outbound create PO from app |

---

## 10. Testing (when implementation authorized)

- Callback on non-RELEASED PR → rejected  
- First callback → LINKED + number stored  
- Same `sourceEventId` → idempotent  
- Different PO number same event → conflict  
- Cross-tenant → 403  
- No SAP network call attempted  
- UI shows NOT_LINKED without inventing number  

---

## 11. Open decisions — **LOCKED 2026-08-15**

| # | Answer |
|---|--------|
| 1 | **Yes** — RELEASED only |
| 2 | **Service JWT** (application auth; gateway mTLS optional infra) |
| 3 | **Yes** — human overwrite with reason |
| 4 | **No** — header only |
| 5 | **P3-09** |

### Original options (archive)


| # | Decision | Options / recommendation |
|---|----------|---------------------------|
| 1 | Require RELEASED only? | **Recommend Yes** |
| 2 | CPI auth model | Service JWT vs gateway mTLS |
| 3 | Allow human overwrite after CPI link? | Recommend yes with reason + audit |
| 4 | Line-level SAP PO items in v1? | **Recommend No** (header only) |
| 5 | Gap number confirmation | **P3-09** (P3-08 reserved Playwright) |

---

## 12. Approval gate

```text
DESIGN ONLY (this document)
        ↓
Product answers §11 + APPROVE FEATURE_P3_09...
        ↓
AUTHORIZE P3-09 IMPLEMENTATION
Scope: e.g. P3-09a only
        ↓
MASTER-Vxxx with schema/API/tests/docs
```

**No runtime code under DESIGN ONLY.**

---

## 13. Traceability

| Existing asset | Role |
|----------------|------|
| `purchase_request.external_po_reference` | Storage target |
| Export package API | Upstream of CPI PO create |
| `POST /api/v1/erp/cpi/purchase-tracking` | Pattern for CPI inbound |
| Live-gate handoff “no SAP MM post” | Unchanged — callback is **inbound confirmation only** |

---

*End of PROPOSED REQUIREMENT — P3-09 CPI SAP PO link callback.*
