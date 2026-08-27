package com.automotive.costing.billing.repository;

import com.automotive.costing.billing.entity.BillingPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BillingPaymentRepository extends JpaRepository<BillingPayment, String> {
    List<BillingPayment> findByCustomerIdOrderByInitiatedAtDesc(String customerId);
    List<BillingPayment> findByStatus(String status);
    Optional<BillingPayment> findByGatewayPaymentRef(String gatewayPaymentRef);
    Optional<BillingPayment> findByIdempotencyKey(String idempotencyKey);
    List<BillingPayment> findByStatusOrderByInitiatedAtAsc(String status);
}
