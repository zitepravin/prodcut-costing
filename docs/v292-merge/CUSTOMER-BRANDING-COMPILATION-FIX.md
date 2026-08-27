# Customer Branding Compilation Fix

| Field | Value |
|-------|-------|
| **Source** | `22082026.zip` (uploaded as V292 master; filename is 22082026.zip, not 220282026.zip) |
| **Date** | 2026-08-23 |
| **Defect** | `CustomerBrandingService` called `getLogoFileId()` / `setLogoFileId()` on `PlatformCustomer`, but the entity had no `logoFileId` mapping |

## Root cause

TC-012 Customer Branding was integrated at the **service and controller** layer:

- `CustomerBrandingService` persists the company logo as a `StoredFile` and stores only the file id on the tenant.
- `CustomerBrandingController` exposes GET/POST/DELETE `/api/v1/customer/branding/logo` with `@permissionAuth.has('CUSTOMER_BRANDING_READ|MANAGE')`.

The **entity and schema were never completed**:

- `PlatformCustomer` had `customerId`, `customerCode`, `customerName`, `status` only.
- No Flyway migration added `platform_customer.logo_file_id`.
- Lombok therefore did not generate `getLogoFileId()` / `setLogoFileId(...)`.
- Maven compile failed with 5 `cannot find symbol` errors.

This is an integration gap, not a Lombok/processor failure.

## Intended architecture (preserved)

```
CustomerContext (tenant)
        ↓
PermissionAuth (CUSTOMER_BRANDING_READ / MANAGE)
        ↓
CustomerBrandingController
        ↓
CustomerBrandingService
        ↓
StoredFileService  (bytes)
PlatformCustomer.logoFileId  (pointer)
```

- One logo property: `logoFileId` ↔ column `logo_file_id`.
- No second branding entity.
- No role-only `hasRole('CUSTOMER_ADMIN')` on these APIs.
- Tenant id comes from `CustomerContext`, not the request body.

## Files changed

| File | Change |
|------|--------|
| `backend/src/main/java/com/automotive/costing/platform/access/entity/PlatformCustomer.java` | Added `logoFileId` mapped to `logo_file_id` CHAR(36) |
| `backend/src/main/resources/db/migration/V165__customer_company_logo.sql` | **New** idempotent `ALTER TABLE` (V164 already used by dashboard; not overwritten) |
| `backend/src/test/java/com/automotive/costing/platform/access/service/CustomerBrandingServiceTest.java` | **New** unit tests (read/upload/clear/validation/tenant isolation) |

Not changed:

- `CustomerBrandingService.java`
- `CustomerBrandingController.java`
- Existing migrations V1–V164
- Permission catalog / `@permissionAuth`
- StoredFileService

## Entity mapping

```java
@Column(name = "logo_file_id", length = 36, columnDefinition = "CHAR(36)")
private String logoFileId;
```

Lombok `@Getter` / `@Setter` on the class generate `getLogoFileId()` / `setLogoFileId(String)`.

## Database compatibility

- `platform_customer` created in V32 **without** `logo_file_id`.
- No prior migration added the column (searched all Flyway scripts).
- Next free version after V164 (dashboard) is **V165**.
- V165 is idempotent (`information_schema` check) so re-apply is safe.
- Existing applied migrations were not edited, renamed, or deleted.

## Tests executed (actual)

### `mvn clean compile`

```
EXIT: 0
```

### `mvn -Dtest=CustomerBrandingServiceTest test`

```
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Coverage: no logo, existing logo, missing tenant context, unknown customer, upload persist, replace previous file, empty file reject, wrong MIME reject, clear logo, clear when none, tenant A does not load tenant B.

### `mvn -Dtest="*Dashboard*" test`

```
DashboardEntitlementTest           2
DashboardServiceVendorKpiTest      7
DashboardTenantIsolationTest       1
DashboardAggregationTest           3
DashboardPermissionTest            1
DashboardControllerTest            1

Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

### `mvn clean test` (full backend)

```
Tests run: 976, Failures: 0, Errors: 0, Skipped: 1
BUILD SUCCESS
Total time: 51.299 s
```

## Remaining blockers

- Live verification **not started** (no running Docker/MySQL/UI stack in this pass).
- Full frontend `npm test` / Playwright E2E not part of this compile-fix task.

## Status

```
CUSTOMER BRANDING FIX   COMPLETE
MAVEN COMPILE           PASS
CUSTOMER BRANDING TESTS PASS (11/11)
DASHBOARD TESTS         PASS (15/15)
FULL BACKEND TESTS      PASS (976 run, 0 fail, 0 error, 1 skipped)
LIVE VERIFICATION       NOT STARTED
```
