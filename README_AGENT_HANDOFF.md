# PRODUCT-COSTING-SOFTWARE — Master Package V93

Structure: `frontend/` + `backend/` + `docs/` only.

## Automated versioning (active)
Should-cost generate, supplier quote ACCEPTED, target-cost activation, **BOM release (ECN)** all write immutable rows to `cost_version_record` without a separate UI action.

## Agent rules
1. Unzip → read PROJECT_STATUS / CURRENT_CONTEXT / NEXT_AGENT
2. Work only in frontend/ and backend/
3. Update docs → produce next MASTER ZIP
4. Flyway max V89; next free V90 only if schema approved
