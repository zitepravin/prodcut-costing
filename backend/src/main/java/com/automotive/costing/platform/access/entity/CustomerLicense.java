package com.automotive.costing.platform.access.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "customer_license")
public class CustomerLicense {
    @Id
    @Column(name = "license_id", length = 36, nullable = false)
    private String licenseId;

    @Column(name = "customer_id", length = 36, nullable = false, unique = true)
    private String customerId;

    @Column(name = "plan_code", length = 50)
    private String planCode;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "total_user_limit", nullable = false)
    private int totalUserLimit;

    @Column(name = "plant_site_limit", nullable = false)
    private int plantSiteLimit;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "grace_end_date")
    private LocalDate graceEndDate;

    @Column(name = "payment_due_date")
    private LocalDate paymentDueDate;

    @Column(name = "over_limit", nullable = false)
    private boolean overLimit;

    @Column(name = "offline_package_id", length = 36)
    private String offlinePackageId;

    @Column(name = "license_version", nullable = false)
    private int licenseVersion;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "updated_by", length = 36)
    private String updatedBy;
}
