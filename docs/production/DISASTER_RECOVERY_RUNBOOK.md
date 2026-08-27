# Disaster Recovery Runbook

**Targets (platform requirement)**  
- **RPO:** ≈ 15 minutes  
- **RTO:** ≈ 60 minutes  

A backup is **not valid** until restore has been successfully tested.

---

## 1. What to back up

| Component | Method | Frequency |
|-----------|--------|-----------|
| MySQL (`product_costing`) | Automated logical dump or managed snapshot + binlog | Continuous binlog + snapshot every ≤ 15 min |
| Object storage (when enabled) | Versioned bucket replication / cross-region | Continuous |
| Secrets (JWT, DB, SMTP) | Secrets manager only — never in git | Continuous |
| Flyway schema | In application repo (migrations V1+) | Version control |

---

## 2. MySQL backup (example — adapt to cloud)

### Logical dump (any environment)

```bash
# Consistent dump
mysqldump -h "$DB_HOST" -u backup_user -p \
  --single-transaction --routines --triggers --set-gtid-purged=OFF \
  product_costing | gzip > "pcs-$(date -u +%Y%m%dT%H%M%SZ).sql.gz"
```

### Point-in-time (preferred production)

- Enable binary logging on primary.
- Retain binlogs ≥ 7 days (policy).
- Combine full snapshot + binlog apply to hit RPO ≤ 15 minutes.

### Off-site

- Copy dumps/snapshots to a second region or account.
- Encrypt at rest (KMS / SSE).

---

## 3. Restore procedure (test quarterly)

1. Provision empty MySQL 8 instance (same major version).
2. Restore latest snapshot/dump.
3. If PITR: apply binlogs to recovery timestamp.
4. Deploy application pointing at restored DB (`SPRING_PROFILES_ACTIVE=prod`).
5. Run Flyway validate (app start) — schema must match.
6. Smoke test:
   - `/actuator/health`
   - Login
   - List programs for DEFAULT / sample tenant
   - Open one BOM / one PR / one PO
7. Record restore duration → compare to RTO 60 min.
8. **Document evidence** (date, operator, duration, pass/fail) in ops ticket.

---

## 4. Application failover

- Stateless backends behind load balancer — replace unhealthy instances via health checks (`/actuator/health`).
- Do not rely on local disk for uploads (object storage).
- Outbox + email queues are DB-backed; after DB restore, workers resume PENDING rows.

---

## 5. Checklist before go-live

- [ ] Automated backup job scheduled  
- [ ] Off-site copy verified  
- [ ] Encryption enabled  
- [ ] Restore drill completed within 60 minutes  
- [ ] Evidence attached to change record  
- [ ] On-call knows this runbook location  

---

## 6. Contacts / escalation

Fill per environment: DBA, platform owner, security.

