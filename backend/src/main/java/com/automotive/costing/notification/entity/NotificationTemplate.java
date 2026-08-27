package com.automotive.costing.notification.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notification_template")
public class NotificationTemplate extends BaseAuditableEntity {

    @Id
    @Column(name = "template_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String templateId;

    @Column(name = "template_code", length = 80, nullable = false)
    private String templateCode;

    @Column(name = "module_code", length = 40)
    private String moduleCode;

    @Column(name = "event_type", length = 40, nullable = false)
    private String eventType;

    @Column(name = "subject_template", length = 500, nullable = false)
    private String subjectTemplate;

    @Column(name = "body_template", length = 4000, nullable = false)
    private String bodyTemplate;

    @Column(name = "recipient_type", length = 40)
    private String recipientType;

    @Column(name = "language_code", length = 10, nullable = false)
    private String languageCode = "en";

    @Column(name = "active_flag", nullable = false)
    private boolean activeFlag = true;
}
