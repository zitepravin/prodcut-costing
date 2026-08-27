package com.automotive.costing.financial.repository;

import com.automotive.costing.financial.entity.ActualCostRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActualCostRecordRepository extends JpaRepository<ActualCostRecord, String> {
    List<ActualCostRecord> findByBomNodeIdOrderByImportedAtDesc(String bomNodeId);
    List<ActualCostRecord> findByCustomerIdAndBomNodeIdOrderByImportedAtDesc(String customerId, String bomNodeId);
    Optional<ActualCostRecord> findByCustomerIdAndActualCostId(String customerId, String actualCostId);
    Optional<ActualCostRecord> findFirstByBomNodeIdOrderByImportedAtDesc(String bomNodeId);
    List<ActualCostRecord> findByBomNodeIdAndPeriodCodeOrderByImportedAtDesc(String bomNodeId, String periodCode);
}
