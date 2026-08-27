package com.automotive.costing.rfq.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "supplier_master",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_supplier_customer_code",
                columnNames = {"customer_id", "supplier_code"}
        )
)
public class SupplierMaster extends BaseAuditableEntity {

    @Id
    @Column(name = "supplier_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String supplierId;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "supplier_code", length = 50, nullable = false)
    private String supplierCode;

    @Column(name = "supplier_name", length = 200, nullable = false)
    private String supplierName;

    @Column(name = "country_code", length = 3)
    private String countryCode;

    @Column(name = "currency_code", length = 3, nullable = false, columnDefinition = "CHAR(3)")
    private String currencyCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private SupplierStatus status;

    @Column(name = "notes", length = 500)
    private String notes;
}
