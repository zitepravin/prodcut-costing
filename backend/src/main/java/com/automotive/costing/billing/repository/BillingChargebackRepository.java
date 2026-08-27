package com.automotive.costing.billing.repository;

import com.automotive.costing.billing.entity.BillingChargeback;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BillingChargebackRepository extends JpaRepository<BillingChargeback, String> {
}
