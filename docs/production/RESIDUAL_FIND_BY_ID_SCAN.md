# Residual findById scan (MASTER-V270)

## Hardened (tenant-scoped) — major domains
- VehicleProgram, BomNode, Plant, Material/Machine/Labor, Process family
- ShouldCost core paths, Tooling records (V140), Packaging, Target cost
- RFQ / SupplierQuote / SupplierMaster / Purchase
- Vendor scorecard, comparison, recommendation, risk, TCO, evidence, questionnaire bank
- Workflow instances, outbox, stored files

## Acceptable / shared platform
- Billing plan catalog, platform_customer registry
- Workflow **definitions** (shared templates)
- User/role/permission
- Benchmark library packs (review if tenant-specific commercial data appears)

## Residual / lower priority
| Area | Risk | Notes |
|------|------|--------|
| SupplierPortalService access/clarification findById | Medium | Portal tokens often opaque; still assert RFQ customer on access |
| Billing payment/subscription findById | Low–Med | Bind via subscription.customerId |
| OverheadPool master | Medium | Same family as material rates — classify then migrate |
| ERP staging tables | Medium | Batch isolation by customer |
| ApprovalStepInstance global queries | Low | Dashboard uses user-scoped pending |

## Process
1. Classify → 2. customer_id if needed → 3. repo method → 4. service require → 5. tests with CustomerContext
