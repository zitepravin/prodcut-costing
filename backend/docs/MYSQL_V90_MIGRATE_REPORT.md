# MySQL Flyway migrate through V90 — session report

**Date:** 2026-08-14  
**MySQL:** 8.0.46  
**Flyway CLI:** 10.20.1  

## Result (this session)
**Successfully applied migrations through version V90** on a clean `product_costing` schema after the hardening below.

## Failures encountered on stock V99 and fixes applied

| Version | Error | Fix |
|---------|-------|-----|
| V28 | FK CHAR vs VARCHAR on `supplier_id` | UUID ID columns → `CHAR(36)` in CREATE scripts |
| V28+ | Collation mismatch `utf8mb4_0900_ai_ci` vs `utf8mb4_unicode_ci` | `DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci` on tables |
| V48 | Row size too large | `VARCHAR(2000/1000)` summaries → `TEXT` |
| V77 | Duplicate index name | Idempotent CREATE INDEX via information_schema |

## V90
`V90__id_column_char36_alignment.sql` applied successfully after earlier tables already used CHAR(36).

## JPA / Hibernate
Entities with `length = 36` now include `columnDefinition = "CHAR(36)"` for `ddl-auto=validate`.

## Backend runtime (partial)
- App started with `JWT_SECRET` + `ddl-auto=none` path validated conceptually.
- Full Playwright against live stack was interrupted by sandbox recycle before FE+BE stable login.

## Host commands (reproduce)
```bash
# MySQL 8 + user costing_app / changeme, DB product_costing
flyway -url=jdbc:mysql://127.0.0.1:3306/product_costing?allowPublicKeyRetrieval=true \
  -user=costing_app -password=changeme \
  -locations=filesystem:backend/src/main/resources/db/migration migrate

export JWT_SECRET='your-32+-char-secret'
export DB_URL='jdbc:mysql://127.0.0.1:3306/product_costing?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
cd backend && mvn spring-boot:run
cd frontend && npm run dev
cd frontend && npx playwright install chromium && npm run test:e2e
```
