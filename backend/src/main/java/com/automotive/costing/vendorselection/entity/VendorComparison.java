package com.automotive.costing.vendorselection.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "vendor_comparison")
public class VendorComparison extends BaseAuditableEntity {
    @Id
    @Column(name = "comparison_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String comparisonId;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;
    @Column(name = "bom_node_id", length = 36, columnDefinition = "CHAR(36)")
    private String bomNodeId;
    @Column(name = "program_id", length = 36, columnDefinition = "CHAR(36)")
    private String programId;
    @Column(name = "currency_code", length = 3, columnDefinition = "CHAR(3)")
    private String currencyCode;
    @Column(name = "supplier_ids_csv", length = 2000, nullable = false)
    private String supplierIdsCsv;
    @Lob
    @Column(name = "comparison_json", columnDefinition = "LONGTEXT", nullable = false)
    private String comparisonJson;
    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "lifecycle_blocked_supplier_ids", length = 2000)
    private String lifecycleBlockedSupplierIds;

    @Column(name = "lifecycle_summary", length = 1000)
    private String lifecycleSummary;
}
