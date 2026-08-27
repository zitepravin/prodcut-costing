package com.automotive.costing.platform.access.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "customer_module_entitlement")
public class CustomerModuleEntitlement extends BaseAuditableEntity {

    @Id
    @Column(name = "entitlement_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String entitlementId;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "module_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String moduleId;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;
}
