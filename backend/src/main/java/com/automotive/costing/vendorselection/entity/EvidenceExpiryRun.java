package com.automotive.costing.vendorselection.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "evidence_expiry_run")
public class EvidenceExpiryRun extends BaseAuditableEntity {

    @Id
    @Column(name = "run_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String runId;

    @Column(name = "ran_at", nullable = false)
    private Instant ranAt;

    @Column(name = "ran_by", length = 36, columnDefinition = "CHAR(36)")
    private String ranBy;

    @Column(name = "as_of_date", nullable = false)
    private LocalDate asOfDate;

    @Column(name = "policies_expired", nullable = false)
    private int policiesExpired;

    @Column(name = "warranties_expired", nullable = false)
    private int warrantiesExpired;

    @Column(name = "guarantees_expired", nullable = false)
    private int guaranteesExpired;

    @Column(name = "notes", length = 1000)
    private String notes;

    /** MANUAL | SCHEDULED */
    @Column(name = "trigger_source", length = 20, nullable = false)
    private String triggerSource = "MANUAL";
}
