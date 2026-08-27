package com.automotive.costing.platform.access.repository;

import com.automotive.costing.platform.access.entity.CustomerFeatureEntitlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerFeatureEntitlementRepository extends JpaRepository<CustomerFeatureEntitlement, String> {
    Optional<CustomerFeatureEntitlement> findByCustomerIdAndFeatureId(String customerId, String featureId);
    List<CustomerFeatureEntitlement> findByCustomerId(String customerId);
}
