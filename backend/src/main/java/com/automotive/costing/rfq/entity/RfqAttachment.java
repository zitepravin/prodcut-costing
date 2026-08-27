package com.automotive.costing.rfq.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "rfq_attachment")
public class RfqAttachment {
    @Id
    @Column(name = "attachment_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String attachmentId;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;
    @Column(name = "rfq_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String rfqId;
    @Column(name = "file_name", length = 255, nullable = false)
    private String fileName;
    @Column(name = "content_type", length = 100)
    private String contentType;
    @Column(name = "storage_uri", length = 1000, nullable = false)
    private String storageUri;
    @Column(name = "uploaded_by", length = 36)
    private String uploadedBy;
    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;
    @Column(name = "notes", length = 500)
    private String notes;
}
