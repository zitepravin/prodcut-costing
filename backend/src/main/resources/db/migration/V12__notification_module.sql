-- M19-adjacent in-app notification module (WF-04 escalation + workflow lifecycle).
CREATE TABLE app_notification (
    notification_id CHAR(36)  NOT NULL,
    recipient_user_id CHAR(36)  NOT NULL,
    notification_type VARCHAR(40)  NOT NULL,
    title             VARCHAR(200) NOT NULL,
    body              VARCHAR(2000) NOT NULL,
    entity_type       VARCHAR(50),
    entity_id CHAR(36),
    severity          VARCHAR(20)  NOT NULL DEFAULT 'INFO',
    read_flag         BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at           DATETIME(6),
    created_by        VARCHAR(36),
    created_at        DATETIME(6)  NOT NULL,
    updated_by        VARCHAR(36),
    updated_at        DATETIME(6),
    PRIMARY KEY (notification_id),
    INDEX idx_notification_recipient_unread (recipient_user_id, read_flag, created_at),
    INDEX idx_notification_recipient_created (recipient_user_id, created_at)
);
