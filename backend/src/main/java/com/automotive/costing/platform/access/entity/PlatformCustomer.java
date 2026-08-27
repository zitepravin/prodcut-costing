package com.automotive.costing.platform.access.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "platform_customer")
public class PlatformCustomer extends BaseAuditableEntity {

    @Id
    @Column(name = "customer_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "customer_code", length = 50, nullable = false)
    private String customerCode;

    @Column(name = "customer_name", length = 200, nullable = false)
    private String customerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private CustomerStatus status = CustomerStatus.ACTIVE;

    /**
     * StoredFile.file_id for the tenant company logo (TC-012). Null = default/no logo.
     * Explicit accessors are kept so compilation does not depend on Lombok generating
     * getLogoFileId/setLogoFileId (the original 5-error compile failure).
     */
    @Column(name = "logo_file_id", length = 36, columnDefinition = "CHAR(36)")
    private String logoFileId;

    public String getLogoFileId() {
        return logoFileId;
    }

    public void setLogoFileId(String logoFileId) {
        this.logoFileId = logoFileId;
    }
}
