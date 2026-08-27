package com.automotive.costing.costingsheet.repository;

import com.automotive.costing.costingsheet.entity.CostingSheetRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CostingSheetRuleRepository extends JpaRepository<CostingSheetRule, String> {
    List<CostingSheetRule> findBySheetIdAndActiveTrueOrderBySequenceNoAsc(String sheetId);
    List<CostingSheetRule> findBySheetIdOrderBySequenceNoAsc(String sheetId);
    long countBySheetId(String sheetId);
}
