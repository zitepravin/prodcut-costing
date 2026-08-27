package com.automotive.costing.targetcosting.repository;

import com.automotive.costing.targetcosting.entity.TargetCostVariance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface TargetCostVarianceRepository extends JpaRepository<TargetCostVariance, String> {

    long countByCustomerIdAndExceedsThresholdTrue(String customerId);

    java.util.List<TargetCostVariance> findByCustomerIdOrderByCalculatedAtDesc(String customerId);


    Optional<TargetCostVariance> findFirstByBomNodeIdOrderByCalculatedAtDesc(String bomNodeId);

    List<TargetCostVariance> findByBomNodeIdOrderByCalculatedAtDesc(String bomNodeId);

    List<TargetCostVariance> findByTargetCostIdOrderByCalculatedAtDesc(String targetCostId);

    long countByExceedsThresholdTrue();

    long countByExceedsThresholdFalse();

    long countByProgramId(String programId);

    long countByProgramIdAndExceedsThresholdTrue(String programId);

    long countByProgramIdAndExceedsThresholdFalse(String programId);

    @Query("select coalesce(sum(v.absoluteVariance), 0) from TargetCostVariance v")
    BigDecimal sumAbsoluteVariance();

    @Query("select coalesce(avg(v.percentageVariance), 0) from TargetCostVariance v")
    BigDecimal avgPercentageVariance();

    @Query("select coalesce(sum(v.absoluteVariance), 0) from TargetCostVariance v where v.programId = :programId")
    BigDecimal sumAbsoluteVarianceByProgramId(@Param("programId") String programId);

    @Query("select coalesce(avg(v.percentageVariance), 0) from TargetCostVariance v where v.programId = :programId")
    BigDecimal avgPercentageVarianceByProgramId(@Param("programId") String programId);
}
