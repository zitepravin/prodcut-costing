package com.automotive.costing.erp.repository;

import com.automotive.costing.erp.entity.ErpPartStaging;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ErpPartStagingRepository extends JpaRepository<ErpPartStaging, String> {
    List<ErpPartStaging> findByBatchIdOrderByCreatedAtAsc(String batchId);
}
