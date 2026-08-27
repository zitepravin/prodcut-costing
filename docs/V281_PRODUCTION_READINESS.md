# V281 Production Readiness

## Status: PRODUCTION READY WITH CONDITIONS

### Measured green
| Gate | Result |
|------|--------|
| Backend compile | SUCCESS |
| Targeted V281/FTA/Import | 37/0/0 |
| Full suite | See V281_FULL_TEST.log |
| FTA → IMPORT_COSTING dependency | Implemented in CentralAccessService |
| Proof lifecycle | create/submit/verify/reject/expire |
| Authoritative import validation | source + officialReference required |
| Allocation perf | 1k ~6ms, 10k ~11ms |

### Conditions (not measured in agent sandbox)
| Condition | Why |
|-----------|-----|
| Live MySQL Flyway V1→V155 | No Docker/MySQL in environment |
| Full HTTP dual-tenant matrix | Requires running stack |
| Backup/restore drill | Ops environment |
| Frontend npm CI | Optional in this wave if Node present |

### Explicit non-goals
No new business modules. FTA remains optional and separate from Normal Import.
