package com.automotive.costing.mixedcost.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * One procurement alternative leg in a mixed-cost version.
 * alternative_type: INTERNAL_SHOULD_COST | SUPPLIER_QUOTE
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "mixed_cost_leg")
@IdClass(MixedCostLeg.MixedCostLegId.class)
public class MixedCostLeg {

    @Id
    @Column(name = "mixed_cost_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String mixedCostId;

    @Id
    @Column(name = "leg_seq", nullable = false)
    private Integer legSeq;

    @Column(name = "alternative_type", length = 40, nullable = false)
    private String alternativeType;

    @Column(name = "source_ref_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String sourceRefId;

    @Column(name = "supplier_id", length = 36, columnDefinition = "CHAR(36)")
    private String supplierId;

    @Column(name = "mix_ratio", precision = 10, scale = 4, nullable = false)
    private BigDecimal mixRatio;

    @Column(name = "unit_cost_snapshot", precision = 18, scale = 6)
    private BigDecimal unitCostSnapshot;

    @Column(name = "currency_snapshot", length = 3, columnDefinition = "CHAR(3)")
    private String currencySnapshot;

    @Column(name = "eligibility_status", length = 20, nullable = false)
    private String eligibilityStatus = "UNKNOWN";

    @Column(name = "eligibility_notes", length = 500)
    private String eligibilityNotes;

    @Column(name = "data_status", length = 20, nullable = false)
    private String dataStatus = "NOT_AVAILABLE";

    @Getter
    @Setter
    @NoArgsConstructor
    public static class MixedCostLegId implements Serializable {
        private String mixedCostId;
        private Integer legSeq;

        public MixedCostLegId(String mixedCostId, Integer legSeq) {
            this.mixedCostId = mixedCostId;
            this.legSeq = legSeq;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MixedCostLegId that)) return false;
            return Objects.equals(mixedCostId, that.mixedCostId) && Objects.equals(legSeq, that.legSeq);
        }

        @Override
        public int hashCode() {
            return Objects.hash(mixedCostId, legSeq);
        }
    }
}
