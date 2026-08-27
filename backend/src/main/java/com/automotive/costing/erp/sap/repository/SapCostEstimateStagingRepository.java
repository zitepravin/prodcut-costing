package com.automotive.costing.erp.sap.repository;

import com.automotive.costing.erp.sap.entity.SapCostEstimateStaging;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SapCostEstimateStagingRepository extends JpaRepository<SapCostEstimateStaging, String> {

    List<SapCostEstimateStaging> findBySapMaterialAndSapPlantOrderByReceivedAtDesc(String sapMaterial, String sapPlant);

    List<SapCostEstimateStaging> findAllByOrderByReceivedAtDesc();

    List<SapCostEstimateStaging> findByBomNodeIdOrderByReceivedAtDesc(String bomNodeId);
}
