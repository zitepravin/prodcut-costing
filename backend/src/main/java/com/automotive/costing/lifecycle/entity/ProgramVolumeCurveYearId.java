package com.automotive.costing.lifecycle.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ProgramVolumeCurveYearId implements Serializable {
    private String curveId;
    private Integer yearIndex;
}
