package com.automotive.costing.purchase.repository;

import com.automotive.costing.purchase.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, String> {

    List<PurchaseOrder> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    List<PurchaseOrder> findByCustomerIdAndStatusOrderByCreatedAtDesc(String customerId, String status);

    Optional<PurchaseOrder> findByPurchaseOrderIdAndCustomerId(String purchaseOrderId, String customerId);

    boolean existsByCustomerIdAndPoNumber(String customerId, String poNumber);

    long countByCustomerIdAndStatusIn(String customerId, Collection<String> statuses);

    long countByCustomerIdAndStatusInAndRequiredDeliveryDateBefore(
            String customerId, Collection<String> statuses, LocalDate before);

    long countByCustomerIdAndBuyerUserIdAndStatusIn(
            String customerId, String buyerUserId, Collection<String> statuses);
}
