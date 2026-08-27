# V295 Flyway Fix Report

## Root cause fixed

The demo-data migrations V167 and V168 were inserting into `audit_log` using
`actor_user_id`, `details`, and `created_at`. The authoritative `audit_log`
schema uses `user_id`, JSON `old_value`/`new_value`, and `timestamp`.

This schema mismatch can cause V167 to fail after most demo-data statements
have executed, producing the reported Flyway failed-migration state.

## Fix

- V167 audit rows now use `user_id`, `new_value` (`JSON_OBJECT('details', ...)`), and `timestamp`.
- V167 idempotency check now uses `user_id`.
- V168 audit volume seed was corrected to the same schema.
- V168 idempotency check now reads the stored detail from `new_value` JSON.
- `backend/pom.xml` has exactly one `<properties>` block.
- Flyway Maven CLI defaults are merged into that existing properties block.
- Flyway Maven plugin is configured for `mvn flyway:*` and command-line `-Dflyway.*` overrides.

## Important database recovery step

If the target database already contains a FAILED V167 entry, the migration
must be repaired only after confirming the failed V167 transaction left no
partial committed DML. V167 contains DML/SET statements only; with InnoDB this
should roll back on failure. Verify the database before running `flyway:repair`.

Then run Flyway validate/migrate against the intended development/demo database.
Do not drop the database or delete `flyway_schema_history`.
