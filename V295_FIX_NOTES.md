# V295 — Flyway Demo Migration Fix

This master preserves the V295 codebase and fixes the V167/V168 demo-data migrations.

## Fixed
- V167 now targets the real `rfq_event` table instead of the nonexistent `rfq` table.
- V167 RFQ rows include required `customer_id` and `title` fields and use schema-valid RFQ statuses.
- V167 tenant-owned plant, supplier, should-cost, and target-cost demo rows now populate required `customer_id` values.
- V167 supplier statuses are constrained to the schema-supported values.
- V168 tenant-owned should-cost, target-cost, and supplier rows now populate required `customer_id` values.
- V168 supplier `PENDING_APPROVAL` is mapped to schema-valid `ACTIVE` while retaining the demo note.
- Existing idempotency/UUID design is preserved.

## Important for a database that already recorded failed V167
Because the previous V167 attempt failed, repair the Flyway history entry before rerunning migration. Do not delete application data manually.

Example:
`mvn flyway:repair -Dflyway.url="jdbc:mysql://127.0.0.1:3306/product_costing" -Dflyway.user="costing_app" -Dflyway.password="changeme"`

Then:
`mvn flyway:info -Dflyway.url="jdbc:mysql://127.0.0.1:3306/product_costing" -Dflyway.user="costing_app" -Dflyway.password="changeme"`

Then:
`mvn flyway:migrate -Dflyway.url="jdbc:mysql://127.0.0.1:3306/product_costing" -Dflyway.user="costing_app" -Dflyway.password="changeme"`
