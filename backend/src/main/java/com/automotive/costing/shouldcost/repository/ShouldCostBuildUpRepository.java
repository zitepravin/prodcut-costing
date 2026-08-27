package com.automotive.costing.shouldcost.repository;

import com.automotive.costing.shouldcost.entity.ShouldCostBuildUp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShouldCostBuildUpRepository extends JpaRepository<ShouldCostBuildUp, String> {

    java.util.Optional<ShouldCostBuildUp> findByCustomerIdAndBuildUpId(String customerId, String buildUpId);

    java.util.List<ShouldCostBuildUp> findByCustomerIdAndBomNodeIdOrderByCalculatedAtDesc(String customerId, String bomNodeId);


    Optional<ShouldCostBuildUp> findByBuildUpId(String buildUpId);

    List<ShouldCostBuildUp> findByBomNodeIdOrderByCalculatedAtDesc(String bomNodeId);

    Optional<ShouldCostBuildUp> findFirstByBomNodeIdAndScenarioLabelOrderByCalculatedAtDesc(
            String bomNodeId, String scenarioLabel);

    @Query("select coalesce(sum(s.materialCost), 0) from ShouldCostBuildUp s")
    BigDecimal sumMaterialCost();

    @Query("select coalesce(sum(s.processCost), 0) from ShouldCostBuildUp s")
    BigDecimal sumProcessCost();

    @Query("select coalesce(sum(s.toolingCost), 0) from ShouldCostBuildUp s")
    BigDecimal sumToolingCost();

    @Query("select coalesce(sum(s.packagingFreightCost), 0) from ShouldCostBuildUp s")
    BigDecimal sumPackagingFreightCost();

    @Query("select coalesce(sum(s.overheadCost), 0) from ShouldCostBuildUp s")
    BigDecimal sumOverheadCost();

    @Query("select coalesce(sum(s.sgaCost), 0) from ShouldCostBuildUp s")
    BigDecimal sumSgaCost();

    @Query("select coalesce(sum(s.profitMargin), 0) from ShouldCostBuildUp s")
    BigDecimal sumProfitMargin();

    /** @deprecated Not tenant-safe — use sumTotalCostByCustomerId */
    @Query("select coalesce(sum(s.totalCost), 0) from ShouldCostBuildUp s")
    BigDecimal sumTotalCost();

    /** Program-scoped aggregates use denormalized program_id (V22) — index-friendly equality. */
    long countByProgramId(String programId);

    /** All build-ups for a program — caller MUST apply one-per-bomNode selection (SC-06). */
    List<ShouldCostBuildUp> findByProgramId(String programId);


    @Query("select coalesce(sum(s.materialCost), 0) from ShouldCostBuildUp s where s.programId = :programId")
    BigDecimal sumMaterialCostByProgramId(@Param("programId") String programId);

    @Query("select coalesce(sum(s.processCost), 0) from ShouldCostBuildUp s where s.programId = :programId")
    BigDecimal sumProcessCostByProgramId(@Param("programId") String programId);

    @Query("select coalesce(sum(s.toolingCost), 0) from ShouldCostBuildUp s where s.programId = :programId")
    BigDecimal sumToolingCostByProgramId(@Param("programId") String programId);

    @Query("select coalesce(sum(s.packagingFreightCost), 0) from ShouldCostBuildUp s where s.programId = :programId")
    BigDecimal sumPackagingFreightCostByProgramId(@Param("programId") String programId);

    @Query("select coalesce(sum(s.overheadCost), 0) from ShouldCostBuildUp s where s.programId = :programId")
    BigDecimal sumOverheadCostByProgramId(@Param("programId") String programId);

    @Query("select coalesce(sum(s.sgaCost), 0) from ShouldCostBuildUp s where s.programId = :programId")
    BigDecimal sumSgaCostByProgramId(@Param("programId") String programId);

    @Query("select coalesce(sum(s.profitMargin), 0) from ShouldCostBuildUp s where s.programId = :programId")
    BigDecimal sumProfitMarginByProgramId(@Param("programId") String programId);

    @Query("select coalesce(sum(s.totalCost), 0) from ShouldCostBuildUp s where s.programId = :programId")
    BigDecimal sumTotalCostByProgramId(@Param("programId") String programId);

    // --- Tenant-scoped aggregates (prefer these over global sum*) ---

    @Query("select coalesce(sum(s.materialCost), 0) from ShouldCostBuildUp s where s.customerId = :customerId")
    BigDecimal sumMaterialCostByCustomerId(@Param("customerId") String customerId);

    @Query("select coalesce(sum(s.processCost), 0) from ShouldCostBuildUp s where s.customerId = :customerId")
    BigDecimal sumProcessCostByCustomerId(@Param("customerId") String customerId);

    @Query("select coalesce(sum(s.toolingCost), 0) from ShouldCostBuildUp s where s.customerId = :customerId")
    BigDecimal sumToolingCostByCustomerId(@Param("customerId") String customerId);

    @Query("select coalesce(sum(s.packagingFreightCost), 0) from ShouldCostBuildUp s where s.customerId = :customerId")
    BigDecimal sumPackagingFreightCostByCustomerId(@Param("customerId") String customerId);

    @Query("select coalesce(sum(s.overheadCost), 0) from ShouldCostBuildUp s where s.customerId = :customerId")
    BigDecimal sumOverheadCostByCustomerId(@Param("customerId") String customerId);

    @Query("select coalesce(sum(s.sgaCost), 0) from ShouldCostBuildUp s where s.customerId = :customerId")
    BigDecimal sumSgaCostByCustomerId(@Param("customerId") String customerId);

    @Query("select coalesce(sum(s.profitMargin), 0) from ShouldCostBuildUp s where s.customerId = :customerId")
    BigDecimal sumProfitMarginByCustomerId(@Param("customerId") String customerId);

    @Query("select coalesce(sum(s.totalCost), 0) from ShouldCostBuildUp s where s.customerId = :customerId")
    BigDecimal sumTotalCostByCustomerId(@Param("customerId") String customerId);

    long countByCustomerId(String customerId);

    List<ShouldCostBuildUp> findByCustomerIdAndProgramId(String customerId, String programId);

    @Query("select coalesce(sum(s.totalCost), 0) from ShouldCostBuildUp s where s.customerId = :customerId and s.programId = :programId")
    BigDecimal sumTotalCostByCustomerIdAndProgramId(@Param("customerId") String customerId, @Param("programId") String programId);

}
