package com.automotive.costing.costingscenario.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "costing_scenario")
public class CostingScenario extends BaseAuditableEntity {

    @Id
    @Column(name = "scenario_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String scenarioId;

    @Column(name = "bom_node_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String bomNodeId;

    @Column(name = "program_id", length = 36, columnDefinition = "CHAR(36)")
    private String programId;

    @Column(name = "scenario_code", length = 40, nullable = false)
    private String scenarioCode;

    @Column(name = "scenario_name", length = 200, nullable = false)
    private String scenarioName;

    @Enumerated(EnumType.STRING)
    @Column(name = "scenario_type", length = 40, nullable = false)
    private ScenarioType scenarioType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ScenarioStatus status = ScenarioStatus.DRAFT;

    @Column(name = "parent_scenario_id", length = 36, columnDefinition = "CHAR(36)")
    private String parentScenarioId;

    @Column(name = "should_cost_build_up_id", length = 36, columnDefinition = "CHAR(36)")
    private String shouldCostBuildUpId;

    @Column(name = "costing_variant_id", length = 36, columnDefinition = "CHAR(36)")
    private String costingVariantId;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "locked", nullable = false)
    private boolean locked;
}
