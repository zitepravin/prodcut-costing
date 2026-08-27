# C1 — Standard Cost Lifecycle

**Package:** MASTER V215

## Lifecycle
```text
DRAFT → MARKED_FOR_RELEASE → RELEASED → FROZEN → SUPERSEDED
```

## Rules
- Standard cost is a **separate object** from should-cost draft
- Create snapshot from should-cost build-up (`from-should-cost`)
- **Release** supersedes previous RELEASED versions (history retained)
- **Freeze** locks a RELEASED baseline for audit
- Never invent unit cost — source must supply total

## API
- `POST /api/v1/standard-costs/from-should-cost`
- `POST .../mark-for-release` | `/release` | `/freeze` | `/supersede`
- `GET /api/v1/standard-costs?bomNodeId=` | `/history`

## UI
Product Costing → Standard Costs (`/standard-costs`)
