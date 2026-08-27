package com.automotive.costing.process.entity;

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

/**
 * PRC-01: a process routing is the ordered list of manufacturing operations that convert
 * raw material into a finished part, for exactly one BOM node. {@code ProcessOperation}
 * rows carry the actual sequence; this entity is the routing's identity/status envelope.
 *
 * A single BOM node may have more than one routing (PRC-07: "side-by-side comparison of
 * two alternate process routings for the same part, e.g. manual vs. automated line") -
 * bomNodeId is therefore NOT unique. {@code ProcessCostService} always resolves "the
 * current routing cost" from the most recently calculated {@code ProcessCostCalculation}
 * for the node, exactly mirroring how M03 handles multiple historical material
 * calculations per node.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "process_routing")
public class ProcessRouting extends BaseAuditableEntity {

    @Id
    @Column(name = "routing_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String routingId;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "bom_node_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String bomNodeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private RoutingStatus status;
}
