# Multi-tenant remaining hardening — V223

## Closed in this pass

### 1. Support READ_ONLY write enforcement
- `SupportReadOnlyFilter` blocks POST/PUT/PATCH/DELETE when support session is active.
- Exception: `/api/v1/platform/support-access/**` (revoke still allowed).
- Rule id: **SUPPORT-03**
- Tests: `SupportReadOnlyFilterTest`

### 2. Production membership policy
- `V120__platform_operator_membership_guidance.sql` documents that platform operators should not hold ACTIVE customer membership by default; use support access instead.
- No automatic wipe of V33 DEFAULT memberships (preserves demo/single-tenant).

## Optional later
- Full permission matrix beyond role PreAuthorize
- UI banner when support session active
