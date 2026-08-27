# DR restore drill evidence template

**Runbook:** `docs/production/DISASTER_RECOVERY_RUNBOOK.md`  
**Targets:** RPO ≈ 15m · RTO ≈ 60m

| Field | Value |
|-------|-------|
| Date | |
| Environment | |
| Backup type (full/incremental) | |
| Backup timestamp | |
| Restore start | |
| Restore end | |
| Duration (RTO) | |
| Data lag vs failure point (RPO) | |
| Application health after restore | |
| Sample tenant data verified | |
| Operator | |
| Issues found | |

Do not mark production-ready until a timed restore completes within RTO and sample tenant isolation still holds.
