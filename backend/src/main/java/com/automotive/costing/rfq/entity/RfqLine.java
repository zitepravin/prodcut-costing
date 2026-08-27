package com.automotive.costing.rfq.entity;

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
@Table(name = "rfq_line")
public class RfqLine extends BaseAuditableEntity {

    @Id
    @Column(name = "rfq_line_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String rfqLineId;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "rfq_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String rfqId;

    @Column(name = "bom_node_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String bomNodeId;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "quantity", precision = 18, scale = 6, nullable = false)
    private BigDecimal quantity;

    @Column(name = "uom", length = 20)
    private String uom;

    @Column(name = "target_unit_price", precision = 18, scale = 6)
    private BigDecimal targetUnitPrice;

    @Column(name = "notes", length = 500)
    private String notes;
}
