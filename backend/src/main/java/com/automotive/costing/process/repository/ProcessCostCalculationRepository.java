package com.automotive.costing.process.repository;

import com.automotive.costing.process.entity.ProcessCostCalculation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessCostCalculationRepository extends JpaRepository<ProcessCostCalculation, String> {

    Optional<ProcessCostCalculation> findFirstByBomNodeIdOrderByCalculatedAtDesc(String bomNodeId);

    List<ProcessCostCalculation> findByRoutingIdOrderByCalculatedAtDesc(String routingId);

    Optional<ProcessCostCalculation> findFirstByCustomerIdAndRoutingIdOrderByCalculatedAtDesc(
            String customerId, String routingId);
}
