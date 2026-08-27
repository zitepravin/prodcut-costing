# Flyway Integration Strategies

**System:** Product Costing + Supplier Intelligence Platform  
**Baseline package:** V278 (migrations V1 → **V153**)  
**Engine:** Flyway (Spring Boot), MySQL 8  

---

## 1. Current implementation (source of truth)

| Setting | Value | Location |
|---------|-------|----------|
| Enabled | `true` | `application.yml`, local, prod |
| Locations | `classpath:db/migration` | `application.yml` |
| Baseline on migrate | `true` | Existing DBs without history |
| Validate on migrate | `true` | Checksum drift detection |
| Clean disabled | `true` | Blocks accidental `clean` in prod |
| JPA `ddl-auto` | `none` (local) / `validate` (prod) | Schema owned by Flyway only |
| Apply path | **Application startup** | Spring Boot auto-migrate |

### Migration inventory

- **~150** versioned scripts under `backend/src/main/resources/db/migration/`
- Latest: **V153** (`industry_profiles_expanded`)
- Universal costing: **V151** (engine tables), **V152** (norms/SOB/commercial/freeze), **V153** (profiles)

### Integration test

`FlywayMigrationIntegrationTest` (`@EnabledIf databaseAvailable`):

1. Prefers host `TEST_DB_URL` / `DB_URL`
2. Else Testcontainers MySQL 8 when Docker is available
3. Else **skips** (unit CI remains green)
4. Asserts `MAX(version) >= 153` in `flyway_schema_history`

---

## 2. Strategy matrix

### Strategy A — Application-managed migrate (CURRENT / RECOMMENDED default)

```text
Deploy new jar/container
        ↓
Spring Boot starts
        ↓
Flyway migrate (pending versions only)
        ↓
App accepts traffic
```

**Use when**

- Single primary DB per environment
- Deploy units are full application releases
- Ops prefers zero separate migrate job

**Pros:** Simple; schema always matches code version that is starting  
**Cons:** Long migrations block startup; failed migrate prevents boot  

**Rules**

- Keep migrations **forward-only**, small, idempotent where possible (`IF NOT EXISTS`, `ON DUPLICATE KEY`)
- Never enable `spring.flyway.clean-disabled=false` in shared environments
- Prod: `jpa.hibernate.ddl-auto=validate` (already)

---

### Strategy B — Migrate-before-traffic (blue/green / rolling)

```text
1. Run Flyway as a one-shot Job / init container (same migration set)
2. Wait for success
3. Roll out application pods (flyway.enabled=true is still safe — no pending work)
   OR set flyway.enabled=false on app if ops owns migrate exclusively
```

**Use when**

- Kubernetes / multi-replica
- Migrations may lock large tables
- Need explicit gate before traffic shift

**Pros:** Startup stays fast; migrate failures do not crash half the fleet  
**Cons:** Two deploy steps; version skew risk if app ships before migrate  

**Rules**

- Job image must contain **exact same** `db/migration` as the app release
- Fail the pipeline if migrate fails; do not proceed to rollout

---

### Strategy C — CI-only validation (no shared DB mutate)

```text
PR / main CI
  → unit tests (Flyway IT skipped without DB)
Release gate / nightly
  → MySQL service
  → FlywayMigrationIntegrationTest (full V1→V153)
```

**Use when**

- Validating scripts without touching staging/prod
- Already implemented in `.github/workflows/release-gate.yml`

**Pros:** Catches broken SQL early  
**Cons:** Does not replace environment migrate  

---

### Strategy D — Expand/contract for zero-downtime

For risky changes (drop column, rename, type change):

```text
Release N:   expand  (add new column/table, dual-write if needed)
Release N+1: migrate code to new shape
Release N+2: contract (drop old column) after all nodes upgraded
```

**Use when**

- Multi-instance rolling deploys
- Large tables that cannot lock long

**Not required** for additive V151–V153 style tables.

---

## 3. Environment playbook

| Environment | Flyway | JPA ddl-auto | How migrate runs |
|-------------|--------|--------------|------------------|
| Local dev | enabled | none | App start (`application-local`) |
| Docker compose | enabled | none | Backend container start |
| Unit CI | n/a | n/a | IT skipped |
| Release CI | enabled | n/a | MySQL service + IT |
| Staging | enabled | validate | App start or init job (B) |
| Production | enabled | validate | Prefer B for multi-replica; A acceptable for single node |

### Host IT (developer / agent)

```bash
export TEST_DB_URL='jdbc:mysql://127.0.0.1:3306/product_costing_it?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
export TEST_DB_USER=costing_app
export TEST_DB_PASSWORD=changeme
cd backend && mvn -B -Dtest=FlywayMigrationIntegrationTest test
```

Or:

```bash
./scripts/ci-verify.sh   # runs IT only if TEST_DB_URL set
```

---

## 4. Versioning conventions (project standard)

1. **One sequential version** per change: `V{n}__short_snake_description.sql`
2. **Never** edit a shipped migration that has run in any shared environment  
   - Fix-forward with `V{n+1}__fix_...sql`
3. Prefer **additive** DDL; use expand/contract for breaking changes
4. Seed data: `INSERT ... ON DUPLICATE KEY UPDATE` (see V151/V153 profiles)
5. Multi-tenant columns: explicit `customer_id` + indexes (V134–V148 pattern)
6. Keep `EXPECTED_MIN_VERSION` in `FlywayMigrationIntegrationTest` equal to **latest** migration number

---

## 5. Repair / incident strategies

| Problem | Action |
|---------|--------|
| Checksum mismatch after illegal edit | **Do not** `flyway repair` in prod without change control; prefer fix-forward script |
| Failed migrate mid-script | Fix SQL; deploy new version; ensure failed version is resolved per Flyway state |
| Need empty DB bootstrap | `baseline-on-migrate: true` allows first start on non-empty legacy DB |
| Accidental clean request | Blocked by `clean-disabled: true` |

Production **must not** use `flyway clean`.

---

## 6. Multi-tenant notes

- Migrations define **shared schema**; tenant isolation is **row-level** (`customer_id`) enforced in services via `TenantAccessGuard`
- Do not create per-tenant schemas via Flyway for this product
- Feature entitlement seeds (V147+) are global catalog rows, not tenant data

---

## 7. CI/CD integration map

| Workflow | Flyway role |
|----------|-------------|
| `.github/workflows/ci.yml` | Unit tests only; IT skips without DB |
| `.github/workflows/release-gate.yml` | MySQL 8 service + `FlywayMigrationIntegrationTest` |
| `.gitlab-ci.yml` | Unit test stage; optional extend with MySQL service |
| `scripts/ci-verify.sh` | Optional host IT when `TEST_DB_URL` set |

---

## 8. Recommended default for this product

```text
Development / single-node deploy  → Strategy A (app-managed)
Kubernetes / multi-replica prod   → Strategy B (migrate job) + A safety net
Every release pipeline            → Strategy C (release-gate IT)
Breaking schema                   → Strategy D (expand/contract)
```

**Do not** enable `out-of-order: true` in production.  
**Do not** use Hibernate `ddl-auto=update` in any environment.

---

## 9. Checklist for a new migration

1. Add `V{next}__description.sql` only (no edits to prior files)
2. Bump `EXPECTED_MIN_VERSION` in `FlywayMigrationIntegrationTest`
3. Update `DATABASE_SCHEMA.md` if structural
4. Run unit tests; run Flyway IT when MySQL available
5. Record in `CHANGELOG.md`

---

## 10. Related files

- `backend/src/main/resources/db/migration/`
- `backend/src/main/resources/application.yml` (flyway block)
- `backend/src/test/java/com/automotive/costing/integration/FlywayMigrationIntegrationTest.java`
- `backend/src/test/java/com/automotive/costing/integration/AbstractMySqlIntegrationTest.java`
- `docs/CI_CD_PIPELINE.md`
- `DATABASE_SCHEMA.md`
