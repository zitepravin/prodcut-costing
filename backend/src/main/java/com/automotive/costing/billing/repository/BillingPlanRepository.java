package com.automotive.costing.billing.repository;

import com.automotive.costing.billing.entity.BillingPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BillingPlanRepository extends JpaRepository<BillingPlan, String> {
    Optional<BillingPlan> findByPlanCode(String planCode);
    List<BillingPlan> findByStatusOrderByPlanCodeAsc(String status);
}
