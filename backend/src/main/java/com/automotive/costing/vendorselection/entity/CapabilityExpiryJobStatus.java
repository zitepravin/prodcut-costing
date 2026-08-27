package com.automotive.costing.vendorselection.entity;

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
@Table(name = "capability_expiry_job_status")
public class CapabilityExpiryJobStatus {
    @Id
    @Column(name = "status_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String statusId;
    @Column(name = "job_name", length = 100, nullable = false)
    private String jobName;
    @Column(name = "enabled_flag", nullable = false)
    private boolean enabledFlag = true;
    @Column(name = "last_scheduled_at")
    private Instant lastScheduledAt;
    @Column(name = "last_run_id", length = 36, columnDefinition = "CHAR(36)")
    private String lastRunId;
    @Column(name = "last_success")
    private Boolean lastSuccess;
    @Column(name = "last_error", length = 2000)
    private String lastError;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
