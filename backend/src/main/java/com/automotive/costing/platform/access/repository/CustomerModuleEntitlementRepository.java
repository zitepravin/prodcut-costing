package com.automotive.costing.platform.access.repository;

import com.automotive.costing.platform.access.entity.CustomerModuleEntitlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerModuleEntitlementRepository extends JpaRepository<CustomerModuleEntitlement, String> {
    List<CustomerModuleEntitlement> findByCustomerId(String customerId);
    Optional<CustomerModuleEntitlement> findByCustomerIdAndModuleId(String customerId, String moduleId);
}
