package com.automotive.costing.billing.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "billing_plan_module")
public class BillingPlanModule extends BaseAuditableEntity {

    @Id
    @Column(name = "plan_module_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String planModuleId;
    @Column(name = "plan_id", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    private String planId;
    @Column(name = "module_id", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    private String moduleId;

}
