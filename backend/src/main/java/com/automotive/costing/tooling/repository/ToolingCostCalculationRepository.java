package com.automotive.costing.tooling.repository;

import com.automotive.costing.tooling.entity.ToolingCostCalculation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ToolingCostCalculationRepository extends JpaRepository<ToolingCostCalculation, String> {

    /** The seam BomNodeService.resolveLeafUnitCost() reads from - mirrors M03/M04's equivalent methods. */
    Optional<ToolingCostCalculation> findFirstByBomNodeIdOrderByCalculatedAtDesc(String bomNodeId);

    Optional<ToolingCostCalculation> findFirstByToolingRecordIdOrderByCalculatedAtDesc(String toolingRecordId);
}
