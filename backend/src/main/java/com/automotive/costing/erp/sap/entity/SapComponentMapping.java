package com.automotive.costing.erp.sap.entity;

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
@Table(name = "sap_component_mapping")
public class SapComponentMapping {

    @Id
    @Column(name = "mapping_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String mappingId;

    @Column(name = "sap_component_code", length = 40, nullable = false, unique = true)
    private String sapComponentCode;

    @Column(name = "our_component_code", length = 40, nullable = false)
    private String ourComponentCode;

    @Column(name = "notes", length = 255)
    private String notes;

    @Column(name = "active_flag", nullable = false)
    private boolean activeFlag = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
