package com.automotive.costing.erp.repository;

import com.automotive.costing.erp.entity.ErpMaterialPriceStaging;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ErpMaterialPriceStagingRepository extends JpaRepository<ErpMaterialPriceStaging, String> {
    List<ErpMaterialPriceStaging> findByBatchIdOrderByCreatedAtAsc(String batchId);
}
