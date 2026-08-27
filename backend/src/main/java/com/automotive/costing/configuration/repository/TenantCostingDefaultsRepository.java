package com.automotive.costing.configuration.repository;

import com.automotive.costing.configuration.entity.TenantCostingDefaults;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantCostingDefaultsRepository extends JpaRepository<TenantCostingDefaults, String> {
    Optional<TenantCostingDefaults> findByCustomerId(String customerId);
}
