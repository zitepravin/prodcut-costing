package com.automotive.costing.billing.repository;

import com.automotive.costing.billing.entity.CustomerSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CustomerSubscriptionRepository extends JpaRepository<CustomerSubscription, String> {
    List<CustomerSubscription> findByCustomerIdOrderByCreatedAtDesc(String customerId);
    List<CustomerSubscription> findByStatus(String status);
}
