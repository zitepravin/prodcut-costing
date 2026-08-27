# Wave E — Central notification event depth (MASTER-V240)

## Goal
Formalize domain → in-app notification path without email in controllers.

## Delivered
- Expanded `NotificationType` catalog (RFQ_*, WORKFLOW_REWORK/CANCELLED/DEADLINE, SYSTEM_ALERT)
- `RfqLifecycleEvent` domain event
- `NotificationEventService` central emit facade (truncate, no secrets, null-safe recipient)
- `RfqNotificationListener` AFTER_COMMIT
- Hooks: `RfqService` create/open → CREATED/SENT; `SupplierQuoteService` submit/accept/reject
- Tests: NotificationEventServiceTest 3, RfqNotificationListenerTest 3

## Unchanged
- Existing workflow/vendor/purchase listeners
- `NotificationDeliveryService` SMTP still optional (Wave F for queue/templates)
- No controller email sending

## Next
Wave F — email templates + delivery queue/retry/preferences
