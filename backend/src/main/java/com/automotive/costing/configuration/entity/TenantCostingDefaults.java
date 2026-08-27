package com.automotive.costing.configuration.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tenant_costing_defaults")
public class TenantCostingDefaults extends BaseAuditableEntity {
    @Id
    @Column(name = "defaults_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String defaultsId;
    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;
    @Column(name = "default_reporting_currency", length = 3, columnDefinition = "CHAR(3)")
    private String defaultReportingCurrency;
    @Column(name = "default_scrap_percent", precision = 9, scale = 4)
    private BigDecimal defaultScrapPercent;
    @Column(name = "default_overhead_percent", precision = 9, scale = 4)
    private BigDecimal defaultOverheadPercent;
    @Column(name = "notes", length = 1000)
    private String notes;
}
