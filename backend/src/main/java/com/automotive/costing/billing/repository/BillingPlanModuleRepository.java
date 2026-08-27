package com.automotive.costing.billing.repository;

import com.automotive.costing.billing.entity.BillingPlanModule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BillingPlanModuleRepository extends JpaRepository<BillingPlanModule, String> {
    List<BillingPlanModule> findByPlanId(String planId);
}
