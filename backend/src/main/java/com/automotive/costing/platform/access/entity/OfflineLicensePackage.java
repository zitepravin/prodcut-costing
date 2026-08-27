package com.automotive.costing.platform.access.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "offline_license_package")
public class OfflineLicensePackage {
    @Id
    @Column(name = "package_id", length = 36, nullable = false)
    private String packageId;

    @Column(name = "customer_id", length = 36, nullable = false)
    private String customerId;

    @Column(name = "package_version", nullable = false)
    private int packageVersion;

    @Column(name = "payload_json", columnDefinition = "MEDIUMTEXT", nullable = false)
    private String payloadJson;

    @Column(name = "signature_hex", length = 128, nullable = false)
    private String signatureHex;

    @Column(name = "signature_alg", length = 20, nullable = false)
    private String signatureAlg;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;

    @Column(name = "imported_by", length = 36)
    private String importedBy;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "status", length = 20, nullable = false)
    private String status;
}
