# V292 FINAL MERGE MANIFEST

| Field | Value |
|-------|-------|
| **Source Master** | testing-V292 (1).zip → PRODUCT-COSTING-SOFTWARE-MASTER-V285 |
| **Defects Package** | artifacts/V292-defects/ (folder; no separate V292-defects.zip on host) |
| **Merge Date** | 2026-08-23 |
| **Output** | PRODUCT-COSTING-SOFTWARE-MASTER-V292-FINAL.zip |

## Merged components

### Support Ticket
- Package `com.automotive.costing.support` (entities, repos, service, controllers, DTOs)
- Migrations V162, V163
- FE: MySupportTicketsPage, SupportTicketDetailPage, Platform support pages, supportTicketApi
- Routes: `/support/my-tickets`, `/support/my-tickets/:ticketId`, `/platform/support/tickets`
- Permissions: SUPPORT_TICKET_READ/CREATE/REPLY/CLOSE/MANAGE/ASSIGN/STATUS_UPDATE/INTERNAL_NOTE
- Notifications: SUPPORT_TICKET_* NotificationType values
- Audit via AuditService; attachments via StoredFileService
- Tests: SupportTicketServiceTest, SupportTicketTenantIsolationTest

### Permission / Customer Entitlement
- EffectivePermissionService
- PermissionAuth (effective + matrix fallback)
- Entities: PermissionCatalogEntry, CustomerPermissionEntitlement, CustomerRolePermission
- Controllers: PlatformEntitlementController, CustomerRolePermissionController
- Migration V161
- FE: CustomerEntitlementsPage, CustomerRolePermissionsPage, entitlementApi

### Phase 3 API wiring
- BomNodeController, RfqSupplierController, ShouldCostController → `@permissionAuth`
- BomPage BOM_EDIT gate
- permissionMatrix.ts extended

### Other defects
- Config pencil (ConfigEditableRow, PlatformConfigurationPage)
- Notifications seed fix V160
- TC-005 PlatformUsersPage multi-select
- TC-012 branding artifacts merged where present

## Migration versions (master latest was V158)

| Version | Purpose |
|---------|---------|
| V160 | Notification seed enum repair |
| V161 | Permission entitlement phase 2 tables + catalog |
| V162 | Support ticket tables + sequence |
| V163 | Support ticket permission catalog seed |

## Tests

| Suite | Status |
|-------|--------|
| Backend SupportTicket* | Present in tree; execution depends on host Maven/JDK |
| Phase3PermissionWiringTest | Present |
| Frontend permission/support tests | Present |
| Full mvn test / npm test | See merge verification run |
| Live verification | **PENDING** (not executed) |

## Known remaining gaps
- Full application not migrated to permissionAuth (~280 role-based PreAuthorize remain)
- Live dual-tenant 403 not executed on this host
- Platform Support UI filters (status/priority) may be partial vs full acceptance checklist
