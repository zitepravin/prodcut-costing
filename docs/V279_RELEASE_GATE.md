# V279 Release Gate

## Status: COMPLETE WITH ACCEPTED RISKS

### Green
- Backend full unit suite: 771 run, 0 fail, 0 error, 1 skip
- Import Costing normal path (no FTA)
- Universal Costing retained
- Allocation performance: 1k @ 4ms, 10k @ 13ms, sum≈charge
- CI workflows present (ci.yml, release-gate.yml, gitlab-ci)

### Accepted risks / residual
| Risk | Mitigation |
|------|------------|
| Flyway IT skipped without DB | release-gate.yml runs MySQL service when executed on GH Actions |
| No live prod migrate proof | Ops dry-run per PRODUCTION_DRY_RUN_CHECKLIST |
| FTA not present | Intentional — V280 |

### Out of scope confirmed
FTA_TRADE_AGREEMENT, RoO, CoO, preferential tariff
