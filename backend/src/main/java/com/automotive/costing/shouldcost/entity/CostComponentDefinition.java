package com.automotive.costing.shouldcost.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cost_component_definition")
public class CostComponentDefinition {

    @Id
    @Column(name = "component_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String componentId;

    @Column(name = "tenant_id", length = 36, columnDefinition = "CHAR(36)")
    private String tenantId;

    @Column(name = "component_code", length = 40, nullable = false)
    private String componentCode;

    @Column(name = "display_name", length = 100, nullable = false)
    private String displayName;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @Column(name = "category", length = 40, nullable = false)
    private String category;

    /** A3: parent in hierarchy (null = root). */
    @Column(name = "parent_component_code", length = 40)
    private String parentComponentCode;

    /** FIXED | VARIABLE | MIXED */
    @Column(name = "cost_behavior", length = 20, nullable = false)
    private String costBehavior = "VARIABLE";

    @Column(name = "hierarchy_level", nullable = false)
    private int hierarchyLevel = 1;

    @Column(name = "maps_to_column", length = 40)
    private String mapsToColumn;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
