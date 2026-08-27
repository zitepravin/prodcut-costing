package com.automotive.costing.costingsheet.repository;

import com.automotive.costing.costingsheet.entity.CostingSheet;
import com.automotive.costing.costingsheet.entity.CostingSheetStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CostingSheetRepository extends JpaRepository<CostingSheet, String> {
    Optional<CostingSheet> findBySheetCode(String sheetCode);
    List<CostingSheet> findAllByOrderBySheetCodeAsc();
    List<CostingSheet> findByStatusOrderBySheetCodeAsc(CostingSheetStatus status);
}
