package com.automotive.costing.vendorselection.entity;

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
@Table(name = "vendor_scorecard_category")
public class VendorScorecardCategory extends BaseAuditableEntity {
    @Id
    @Column(name = "category_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String categoryId;
    @Column(name = "template_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String templateId;
    @Column(name = "category_code", length = 40, nullable = false)
    private String categoryCode;
    @Column(name = "category_name", length = 150, nullable = false)
    private String categoryName;
    @Column(name = "weight_percent", precision = 9, scale = 4, nullable = false)
    private BigDecimal weightPercent;
    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;
}
