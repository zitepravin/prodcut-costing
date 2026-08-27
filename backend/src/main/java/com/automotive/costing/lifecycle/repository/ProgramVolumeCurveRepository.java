package com.automotive.costing.lifecycle.repository;

import com.automotive.costing.lifecycle.entity.ProgramVolumeCurve;
import com.automotive.costing.lifecycle.entity.VolumeCurveStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProgramVolumeCurveRepository extends JpaRepository<ProgramVolumeCurve, String> {
    List<ProgramVolumeCurve> findByProgramIdOrderByScenarioCodeAsc(String programId);

    Optional<ProgramVolumeCurve> findByProgramIdAndScenarioCodeAndStatus(
            String programId, String scenarioCode, VolumeCurveStatus status);

    Optional<ProgramVolumeCurve> findFirstByProgramIdAndScenarioCodeOrderByCreatedAtDesc(
            String programId, String scenarioCode);
}
