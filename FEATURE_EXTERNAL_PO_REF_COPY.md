# Copy PR.externalPoReference → PO on seed (MASTER-V247)

## Behavior
When creating a PO linked to a purchase request (`create` with `purchaseRequestId`, or `createFromPurchaseRequest`):

- If PR has non-blank `externalPoReference`, copy it onto the new PO.
- If PR has none, PO `externalPoReference` stays null (no invention).
- **No SAP MM call.** Label `SAP_MM` on PR remains storage metadata only.

## Naming note
- SAP link API field: `externalPoNumber`
- PR/PO storage field: `externalPoReference`
- CPI/human link services map number → `externalPoReference` on the PR; this change only copies that stored value to the PO.

## Issue still independent
Human may overwrite/set `externalPoReference` again on **Issue** via `IssuePoRequest`.
