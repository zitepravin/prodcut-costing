package com.automotive.costing.purchase.repository;

import com.automotive.costing.purchase.entity.PurchaseRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, String> {

    Optional<PurchaseRequest> findByCustomerIdAndPurchaseRequestId(String customerId, String purchaseRequestId);

    List<PurchaseRequest> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<PurchaseRequest> findByCustomerIdAndBomNodeIdOrderByCreatedAtDesc(String customerId, String bomNodeId);

    List<PurchaseRequest> findByCustomerIdAndProgramIdOrderByCreatedAtDesc(String customerId, String programId);

    long countByCustomerIdAndStatusNotIn(String customerId, Collection<String> statuses);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<PurchaseRequest> findByBomNodeIdOrderByCreatedAtDesc(String bomNodeId);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<PurchaseRequest> findByProgramIdOrderByCreatedAtDesc(String programId);
}
