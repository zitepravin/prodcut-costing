# QA / Tester Suite — Vendor Selection

Automated tests that mirror manual tester scenarios.

## How to run

```bash
# All vendor-selection + audit unit/QA tests
mvn -DskipITs test -Dtest='com.automotive.costing.vendorselection.**,com.automotive.costing.common.audit.**'

# Transition engine coverage gate
mvn -DskipITs test verify -Dtest='*TransitionEngine*'

# Frontend utils + QA mirrors
cd ../product-costing-frontend && npm run test:coverage
```

## Scenario catalogue (DisplayName prefix `QA:`)

| Area | What tester validates |
|------|------------------------|
| Warranty claim status | Accept OPEN; CLOSED terminal; evidence verify ≠ claim status; AND filters; summary buckets |
| Guarantee events | Unresolved+PENDING AND filter; resolved-only |
| Risk register | ACCEPTED needs mitigation; OPEN→MITIGATING audits |
| Recommendation | Approve only SUBMITTED; no approve DRAFT/APPROVED; override needs reason+supplier |
| Lifecycle | No DISCOVERED→APPROVED; APPROVED only from APPROVAL_PENDING; BLOCKED→UNDER_VERIFICATION only |
| Lifecycle guard | BLOCKED cannot score; QUALIFIED can TCO |
| Policy evidence | Declaration-only cannot VERIFIED without ref; WAIVED needs reason |
| Evidence/Risk/Claim engines | Full allowed/forbidden matrix |

## Manual tester checklist (UI)

1. Login as non-admin → Audit page denied.
2. Vendor Selection 360 → load supplier → claim filters OPEN+PENDING.
3. Accept claim → status ACCEPTED; evidence still PENDING until Verify.
4. Recommendation queue → only SUBMITTED shows approve/reject.
5. Dashboard KPI links open Vendor Selection tabs.
