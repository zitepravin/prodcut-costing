package com.automotive.costing.costingscenario.repository;

import com.automotive.costing.costingscenario.entity.CostingScenario;
import com.automotive.costing.costingscenario.entity.ScenarioStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CostingScenarioRepository extends JpaRepository<CostingScenario, String> {
    List<CostingScenario> findByBomNodeIdOrderByScenarioCodeAsc(String bomNodeId);
    Optional<CostingScenario> findByBomNodeIdAndScenarioCode(String bomNodeId, String scenarioCode);
    List<CostingScenario> findByBomNodeIdAndStatus(String bomNodeId, ScenarioStatus status);
}
