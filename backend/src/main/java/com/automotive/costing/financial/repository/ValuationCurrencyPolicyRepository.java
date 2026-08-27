package com.automotive.costing.financial.repository;

import com.automotive.costing.financial.entity.ValuationCurrencyPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ValuationCurrencyPolicyRepository extends JpaRepository<ValuationCurrencyPolicy, String> {
    Optional<ValuationCurrencyPolicy> findByPolicyCode(String policyCode);
    List<ValuationCurrencyPolicy> findByActiveTrueOrderByPolicyCodeAsc();
}
