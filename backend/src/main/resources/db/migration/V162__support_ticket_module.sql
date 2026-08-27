-- V162: Customer Support Ticket module (tenant-isolated)

CREATE TABLE IF NOT EXISTS support_ticket (
    ticket_id       CHAR(36)      NOT NULL,
    ticket_number   VARCHAR(20)   NOT NULL,
    customer_id     CHAR(36)      NOT NULL,
    created_by      CHAR(36)      NOT NULL,
    subject         VARCHAR(200)  NOT NULL,
    description     VARCHAR(4000) NOT NULL,
    category        VARCHAR(40)   NOT NULL,
    priority        VARCHAR(20)   NOT NULL,
    status          VARCHAR(30)   NOT NULL,
    assigned_to     CHAR(36)      NULL,
    version         BIGINT        NOT NULL DEFAULT 0,
    created_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)   NULL,
    resolved_at     DATETIME(6)   NULL,
    closed_at       DATETIME(6)   NULL,
    created_by_user VARCHAR(64)   NULL,
    updated_by      VARCHAR(64)   NULL,
    PRIMARY KEY (ticket_id),
    UNIQUE KEY uq_support_ticket_number (ticket_number),
    KEY idx_st_customer_status (customer_id, status),
    KEY idx_st_customer_created (customer_id, created_at),
    KEY idx_st_assigned (assigned_to),
    CONSTRAINT fk_st_customer FOREIGN KEY (customer_id) REFERENCES platform_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS support_ticket_message (
    message_id      CHAR(36)      NOT NULL,
    ticket_id       CHAR(36)      NOT NULL,
    author_user_id  CHAR(36)      NOT NULL,
    author_type     VARCHAR(20)   NOT NULL,
    message_body    VARCHAR(4000) NOT NULL,
    created_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (message_id),
    KEY idx_stm_ticket_created (ticket_id, created_at),
    CONSTRAINT fk_stm_ticket FOREIGN KEY (ticket_id) REFERENCES support_ticket (ticket_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS support_ticket_internal_note (
    note_id         CHAR(36)      NOT NULL,
    ticket_id       CHAR(36)      NOT NULL,
    author_user_id  CHAR(36)      NOT NULL,
    note_body       VARCHAR(4000) NOT NULL,
    created_at      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (note_id),
    KEY idx_stin_ticket (ticket_id, created_at),
    CONSTRAINT fk_stin_ticket FOREIGN KEY (ticket_id) REFERENCES support_ticket (ticket_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS support_ticket_seq (
    seq_name        VARCHAR(40)   NOT NULL,
    next_val        BIGINT        NOT NULL,
    PRIMARY KEY (seq_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO support_ticket_seq (seq_name, next_val) VALUES ('SUPPORT_TICKET', 0)
ON DUPLICATE KEY UPDATE seq_name = seq_name;
