-- V121: Customer Admin — profile fields, departments, user invitations (tenant-scoped)

-- Profile / employee fields on app_user (nullable; invitation flow may leave password null)
ALTER TABLE app_user
    ADD COLUMN display_name   VARCHAR(200) NULL AFTER email,
    ADD COLUMN employee_id    VARCHAR(50)  NULL AFTER display_name,
    ADD COLUMN designation    VARCHAR(100) NULL AFTER employee_id,
    ADD COLUMN department_id  CHAR(36)     NULL AFTER designation,
    ADD COLUMN mobile_number  VARCHAR(30)  NULL AFTER department_id;

-- Status may be Active | Inactive | Invited (application enforces values)

CREATE TABLE customer_department (
    department_id   CHAR(36)     NOT NULL PRIMARY KEY,
    customer_id     CHAR(36)     NOT NULL,
    department_code VARCHAR(50)  NOT NULL,
    department_name VARCHAR(150) NOT NULL,
    description     VARCHAR(500) NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_by      VARCHAR(36),
    created_at      DATETIME(6)  NOT NULL,
    updated_by      VARCHAR(36),
    updated_at      DATETIME(6),
    CONSTRAINT uk_customer_department_code UNIQUE (customer_id, department_code),
    CONSTRAINT fk_customer_department_customer FOREIGN KEY (customer_id)
        REFERENCES platform_customer (customer_id)
) ENGINE = InnoDB;

CREATE INDEX idx_customer_department_customer ON customer_department (customer_id, status);

CREATE TABLE user_invitation (
    invitation_id   CHAR(36)     NOT NULL PRIMARY KEY,
    customer_id     CHAR(36)     NOT NULL,
    user_id         CHAR(36)     NOT NULL,
    email           VARCHAR(150) NOT NULL,
    token_hash      VARCHAR(64)  NOT NULL,
    expires_at      DATETIME(6)  NOT NULL,
    accepted_at     DATETIME(6)  NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_by      VARCHAR(36),
    created_at      DATETIME(6)  NOT NULL,
    updated_by      VARCHAR(36),
    updated_at      DATETIME(6),
    CONSTRAINT uk_user_invitation_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_user_invitation_customer FOREIGN KEY (customer_id)
        REFERENCES platform_customer (customer_id),
    CONSTRAINT fk_user_invitation_user FOREIGN KEY (user_id)
        REFERENCES app_user (user_id)
) ENGINE = InnoDB;

CREATE INDEX idx_user_invitation_customer ON user_invitation (customer_id, status);
CREATE INDEX idx_user_invitation_user ON user_invitation (user_id);
