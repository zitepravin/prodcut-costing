package com.automotive.costing.material.repository;

import com.automotive.costing.material.entity.MaterialCostCalculation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface MaterialCostCalculationRepository extends JpaRepository<MaterialCostCalculation, String> {

    /**
     * The seam BomNodeService.resolveLeafUnitCost() reads from - "the current material
     * cost for this BOM node" is always the most recently calculated row, never an
     * aggregate or an average.
     */
    Optional<MaterialCostCalculation> findFirstByBomNodeIdOrderByCalculatedAtDesc(String bomNodeId);

    /** MAT-07: 24-month trend reporting per material grade. */
    List<MaterialCostCalculation> findByMaterialCodeAndCalculatedAtAfterOrderByCalculatedAtDesc(
            String materialCode, Instant since);

    List<MaterialCostCalculation> findByMaterialCodeOrderByCalculatedAtDesc(String materialCode);
}
