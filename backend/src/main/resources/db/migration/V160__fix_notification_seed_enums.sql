-- V160: Repair invalid app_notification enum values from V18 demo seed.
-- V18 inserted notification_type='TARGET_VARIANCE' (now in enum) and severity='WARN'
-- (invalid; enum is INFO/WARNING/CRITICAL). Hibernate EnumType.STRING throws on list,
-- producing HTTP 500 + "An unexpected error occurred..." for demo.manager.

UPDATE app_notification
SET severity = 'WARNING'
WHERE severity = 'WARN';

-- Any legacy/unknown types that are not in the Java enum will still break reads;
-- map empty/null-safe and leave known TARGET_VARIANCE as-is after enum add.
UPDATE app_notification
SET notification_type = 'SYSTEM_ALERT'
WHERE notification_type IS NULL OR notification_type = '';
