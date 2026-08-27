package com.automotive.costing.lifecycle.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "program_volume_curve")
public class ProgramVolumeCurve extends BaseAuditableEntity {

    @Id
    @Column(name = "curve_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String curveId;

    @Column(name = "program_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String programId;

    @Column(name = "scenario_code", length = 40, nullable = false)
    private String scenarioCode;

    @Column(name = "scenario_label", length = 120)
    private String scenarioLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private VolumeCurveStatus status = VolumeCurveStatus.DRAFT;

    @Column(name = "notes", length = 500)
    private String notes;
}
