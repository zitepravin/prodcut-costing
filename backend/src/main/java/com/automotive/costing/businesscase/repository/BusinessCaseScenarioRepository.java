package com.automotive.costing.businesscase.repository;

import com.automotive.costing.businesscase.entity.BusinessCaseScenario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BusinessCaseScenarioRepository extends JpaRepository<BusinessCaseScenario, String> {
    List<BusinessCaseScenario> findByProgramIdOrderByScenarioCodeAsc(String programId);
    Optional<BusinessCaseScenario> findByProgramIdAndScenarioCode(String programId, String scenarioCode);
}
