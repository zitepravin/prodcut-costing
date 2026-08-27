package com.automotive.costing.costingscenario.repository;

import com.automotive.costing.costingscenario.entity.AdditiveCostLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdditiveCostLineRepository extends JpaRepository<AdditiveCostLine, String> {
    List<AdditiveCostLine> findByScenarioIdOrderByLineCodeAsc(String scenarioId);
    void deleteByScenarioIdAndLineCode(String scenarioId, String lineCode);
}
