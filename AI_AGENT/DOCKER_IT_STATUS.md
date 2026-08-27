# Docker / Host Integration Test Status

## FlywayMigrationIntegrationTest

| Mode | Requirement | Status (2026-08-14) |
|------|-------------|---------------------|
| Host MySQL/MariaDB | `TEST_DB_URL` + credentials | **PASS** — V1→V90, ERP + CAM tables |
| Testcontainers | Docker available | Use when Docker present (no host URL) |
| Neither | — | Test **skipped** via `@EnabledIf` |

### Host run

```bash
export TEST_DB_URL='jdbc:mysql://127.0.0.1:3306/product_costing_it?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
export TEST_DB_USERNAME=costing_app
export TEST_DB_PASSWORD=changeme
mvn -Dtest=FlywayMigrationIntegrationTest test
```

Asserts:
- `flyway_schema_history` max version ≥ **90**
- ERP staging tables (4)
- Central Access tables (4)
