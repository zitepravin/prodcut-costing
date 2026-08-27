package com.automotive.costing.billing.repository;

import com.automotive.costing.billing.entity.BillingInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BillingInvoiceRepository extends JpaRepository<BillingInvoice, String> {
    List<BillingInvoice> findByCustomerIdOrderByInvoiceDateDesc(String customerId);
    List<BillingInvoice> findBySubscriptionId(String subscriptionId);
}
