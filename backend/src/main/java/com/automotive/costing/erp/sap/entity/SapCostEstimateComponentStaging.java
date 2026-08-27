package com.automotive.costing.erp.sap.entity;

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
@Table(name = "sap_cost_estimate_component_staging")
@IdClass(SapCostEstimateComponentStaging.Pk.class)
public class SapCostEstimateComponentStaging {

    @Id
    @Column(name = "staging_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String stagingId;

    @Id
    @Column(name = "component_seq", nullable = false)
    private Integer componentSeq;

    @Column(name = "sap_component_code", length = 40, nullable = false)
    private String sapComponentCode;

    @Column(name = "sap_component_name", length = 120)
    private String sapComponentName;

    @Column(name = "amount", precision = 18, scale = 6, nullable = false)
    private BigDecimal amount;

    @Column(name = "mapped_component_code", length = 40)
    private String mappedComponentCode;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Pk implements Serializable {
        private String stagingId;
        private Integer componentSeq;

        public Pk(String stagingId, Integer componentSeq) {
            this.stagingId = stagingId;
            this.componentSeq = componentSeq;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pk pk)) return false;
            return Objects.equals(stagingId, pk.stagingId) && Objects.equals(componentSeq, pk.componentSeq);
        }

        @Override
        public int hashCode() {
            return Objects.hash(stagingId, componentSeq);
        }
    }
}
