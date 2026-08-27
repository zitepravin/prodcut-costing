# Purchase module — permanent non-goals

These are **product policy**, not temporary backlog items. Do not implement them under "NEXT".

| Non-goal | Policy |
|----------|--------|
| **Not ERP goods receipt (GR)** | `PARTIALLY_RECEIVED` / `CLOSED` are **operational status markers only**. No GR document, no movement type, no stock posting. |
| **Not invoice receipt (IR)** | No invoice matching, no 3-way match engine, no AP posting. |
| **Not SAP MM posting** | No live ME21N / BAPI / IDoc post. `externalPoReference` is reference/export only. |
| **No invented quantities or prices** | Missing unit price → `dataStatus = NOT_AVAILABLE`. **Issue is blocked** until all lines have real prices. Never invent FX or unit price. |

## Allowed (already delivered)
- PR lifecycle + human approve/release
- PR → RFQ (reuse existing RFQ)
- PR → PO draft seed
- PO: DRAFT → … → ISSUED → PARTIALLY_RECEIVED / CLOSED
- Module entitlement, tenant isolation, support read-only
- Export/reference fields for external CPI/ERP (human-driven)

## AI boundary
AI may explain status and missing data. AI must **never** approve, issue, close, invent prices, or post to ERP.
