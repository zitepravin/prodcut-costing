package com.automotive.costing.tooling.repository;

import com.automotive.costing.tooling.entity.ToolingAssetStatus;
import com.automotive.costing.tooling.entity.ToolingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ToolingRecordRepository extends JpaRepository<ToolingRecord, String> {

    Optional<ToolingRecord> findByCustomerIdAndToolingRecordId(String customerId, String toolingRecordId);

    List<ToolingRecord> findByCustomerIdAndBomNodeId(String customerId, String bomNodeId);

    List<ToolingRecord> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<ToolingRecord> findByCustomerIdAndPlantLocation(String customerId, String plantLocation);

    long countByCustomerIdAndAssetStatus(String customerId, ToolingAssetStatus assetStatus);

    @Query("select coalesce(sum(t.toolCost), 0) from ToolingRecord t "
            + "where t.customerId = :customerId and t.assetStatus = :assetStatus")
    BigDecimal sumToolCostByCustomerIdAndAssetStatus(
            @Param("customerId") String customerId, @Param("assetStatus") ToolingAssetStatus assetStatus);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<ToolingRecord> findByBomNodeId(String bomNodeId);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    Optional<ToolingRecord> findByToolingRecordId(String toolingRecordId);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<ToolingRecord> findByAssetStatus(ToolingAssetStatus assetStatus);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<ToolingRecord> findByPlantLocationAndAssetStatus(String plantLocation, ToolingAssetStatus assetStatus);
}
