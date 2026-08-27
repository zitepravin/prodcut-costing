package com.automotive.costing.erp.repository;

import com.automotive.costing.erp.entity.ErpBatchStatus;
import com.automotive.costing.erp.entity.ErpInboundBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ErpInboundBatchRepository extends JpaRepository<ErpInboundBatch, String> {

    Optional<ErpInboundBatch> findByCustomerIdAndBatchId(String customerId, String batchId);

    List<ErpInboundBatch> findByCustomerIdOrderByReceivedAtDesc(String customerId);

    Optional<ErpInboundBatch> findByBatchId(String batchId);
    List<ErpInboundBatch> findByStatusOrderByReceivedAtDesc(ErpBatchStatus status);
    List<ErpInboundBatch> findAllByOrderByReceivedAtDesc();

    long countByStatus(ErpBatchStatus status);

    @Query("select b.status, count(b) from ErpInboundBatch b group by b.status")
    List<Object[]> countGroupedByStatus();
}
