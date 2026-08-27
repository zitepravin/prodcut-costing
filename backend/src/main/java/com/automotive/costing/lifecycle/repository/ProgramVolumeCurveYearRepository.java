package com.automotive.costing.lifecycle.repository;

import com.automotive.costing.lifecycle.entity.ProgramVolumeCurveYear;
import com.automotive.costing.lifecycle.entity.ProgramVolumeCurveYearId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProgramVolumeCurveYearRepository extends JpaRepository<ProgramVolumeCurveYear, ProgramVolumeCurveYearId> {
    List<ProgramVolumeCurveYear> findByCurveIdOrderByYearIndexAsc(String curveId);

    void deleteByCurveId(String curveId);
}
