package com.automotive.costing.erp.repository;

import com.automotive.costing.erp.entity.ErpBomStaging;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ErpBomStagingRepository extends JpaRepository<ErpBomStaging, String> {
    List<ErpBomStaging> findByBatchIdOrderByLineNumberAsc(String batchId);
}
