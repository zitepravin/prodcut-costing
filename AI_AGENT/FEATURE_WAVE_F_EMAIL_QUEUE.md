# Wave F — Email templates + delivery queue (MASTER-V241)

## Delivered
- Flyway **V128**: `notification_template`, `notification_delivery`, `notification_preference` + seed templates
- `EmailTemplateRenderer` — `{{placeholders}}` only; secrets redacted; no code execution
- `EmailDeliveryQueueService` — enqueue with idempotency key `notif:{notificationId}`; statuses QUEUED→PROCESSING→SENT|FAILED|RETRYING
- `NotificationPreferenceService` — module entitlement gate (`PREF_MODULE_NOT_ENTITLED`)
- `NotificationDeliveryService` → queue (no direct SMTP from publish path)
- `EmailDeliveryScheduler` — poll when `app.notification.email-enabled=true`
- Tests: 15 PASS (renderer 4, queue 3, preference 2, Wave E regression 6)

## Policy
- Controllers never send email
- No passwords/JWTs/tokens in email body
- Preferences for unsubscribed modules rejected when tenant context present
- SMTP still optional (email-enabled default false)

## Next
Wave G — security expansion (PO 403, cross-tenant, preference module)
