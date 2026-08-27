package com.automotive.costing.masterdata.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "vehicle_program",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_vehicle_program_customer_code",
                columnNames = {"customer_id", "program_code"}
        )
)
public class VehicleProgram extends BaseAuditableEntity {

    @Id
    @Column(name = "program_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String programId;

    /** Tenant owner — set from CustomerContext only. */
    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "program_code", length = 50, nullable = false)
    private String programCode;

    @Column(name = "model_name", length = 100)
    private String modelName;

    @Column(name = "platform_code", length = 50)
    private String platformCode;

    @Column(name = "sourcing_region", length = 50)
    private String sourcingRegion;

    @Column(name = "lifetime_volume", precision = 18, scale = 2)
    private BigDecimal lifetimeVolume;

    @Column(name = "target_cost", precision = 18, scale = 6)
    private BigDecimal targetCost;

    @Column(name = "target_currency", length = 3, columnDefinition = "CHAR(3)")
    private String targetCurrency;

    @Column(name = "status", length = 20, nullable = false)
    private String status;
}
