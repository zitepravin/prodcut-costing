package com.automotive.costing.makevsbuy.entity;

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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "make_vs_buy_leg")
@IdClass(MakeVsBuyLeg.Pk.class)
public class MakeVsBuyLeg {

    @Id
    @Column(name = "simulation_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String simulationId;

    @Id
    @Column(name = "leg_seq", nullable = false)
    private Integer legSeq;

    @Column(name = "strategy_type", length = 20, nullable = false)
    private String strategyType;

    @Column(name = "source_ref_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String sourceRefId;

    @Column(name = "supplier_id", length = 36, columnDefinition = "CHAR(36)")
    private String supplierId;

    @Column(name = "unit_cost_snapshot", precision = 18, scale = 6)
    private BigDecimal unitCostSnapshot;

    @Column(name = "currency_snapshot", length = 3, columnDefinition = "CHAR(3)")
    private String currencySnapshot;

    @Column(name = "tco_unit_snapshot", precision = 18, scale = 6)
    private BigDecimal tcoUnitSnapshot;

    @Column(name = "tco_status", length = 20, nullable = false)
    private String tcoStatus = "NOT_AVAILABLE";

    @Column(name = "eligibility_status", length = 20, nullable = false)
    private String eligibilityStatus = "UNKNOWN";

    @Column(name = "eligibility_notes", length = 500)
    private String eligibilityNotes;

    @Column(name = "risk_summary", length = 500)
    private String riskSummary;

    @Column(name = "quality_score_snapshot", precision = 10, scale = 4)
    private BigDecimal qualityScoreSnapshot;

    @Column(name = "delivery_score_snapshot", precision = 10, scale = 4)
    private BigDecimal deliveryScoreSnapshot;

    @Column(name = "data_status", length = 20, nullable = false)
    private String dataStatus = "NOT_AVAILABLE";

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Pk implements Serializable {
        private String simulationId;
        private Integer legSeq;

        public Pk(String simulationId, Integer legSeq) {
            this.simulationId = simulationId;
            this.legSeq = legSeq;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pk pk)) return false;
            return Objects.equals(simulationId, pk.simulationId) && Objects.equals(legSeq, pk.legSeq);
        }

        @Override
        public int hashCode() {
            return Objects.hash(simulationId, legSeq);
        }
    }
}
