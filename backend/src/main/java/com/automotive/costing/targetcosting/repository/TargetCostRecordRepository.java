package com.automotive.costing.targetcosting.repository;

import com.automotive.costing.targetcosting.entity.TargetCostRecord;
import com.automotive.costing.targetcosting.entity.TargetCostStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TargetCostRecordRepository extends JpaRepository<TargetCostRecord, String> {

    Optional<TargetCostRecord> findByCustomerIdAndTargetCostId(String customerId, String targetCostId);

    List<TargetCostRecord> findByCustomerIdAndBomNodeIdOrderByEffectiveFromDesc(String customerId, String bomNodeId);

    List<TargetCostRecord> findByCustomerIdAndProgramIdOrderByEffectiveFromDesc(String customerId, String programId);

    long countByCustomerIdAndStatus(String customerId, TargetCostStatus status);


    Optional<TargetCostRecord> findByTargetCostId(String targetCostId);

    List<TargetCostRecord> findByBomNodeIdOrderByEffectiveFromDesc(String bomNodeId);

    List<TargetCostRecord> findByProgramIdOrderByEffectiveFromDesc(String programId);

    Optional<TargetCostRecord> findFirstByBomNodeIdAndStatusOrderByEffectiveFromDesc(
            String bomNodeId, TargetCostStatus status);

    long countByStatus(TargetCostStatus status);

    long countByProgramIdAndStatus(String programId, TargetCostStatus status);
}
