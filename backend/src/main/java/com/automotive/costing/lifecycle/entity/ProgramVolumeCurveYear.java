package com.automotive.costing.lifecycle.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "program_volume_curve_year")
@IdClass(ProgramVolumeCurveYearId.class)
public class ProgramVolumeCurveYear {

    @Id
    @Column(name = "curve_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String curveId;

    @Id
    @Column(name = "year_index", nullable = false)
    private Integer yearIndex;

    @Column(name = "calendar_year")
    private Integer calendarYear;

    @Column(name = "planned_volume", precision = 18, scale = 2, nullable = false)
    private BigDecimal plannedVolume;
}
