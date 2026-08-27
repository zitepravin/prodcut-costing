package com.automotive.costing.erp.sap.repository;

import com.automotive.costing.erp.sap.entity.SapCostEstimateComponentStaging;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SapCostEstimateComponentStagingRepository
        extends JpaRepository<SapCostEstimateComponentStaging, SapCostEstimateComponentStaging.Pk> {

    List<SapCostEstimateComponentStaging> findByStagingIdOrderByComponentSeqAsc(String stagingId);

    void deleteByStagingId(String stagingId);
}
