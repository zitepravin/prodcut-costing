package com.automotive.costing.packaging.repository;

import com.automotive.costing.packaging.entity.PackagingLogisticsCostCalculation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PackagingLogisticsCostCalculationRepository
        extends JpaRepository<PackagingLogisticsCostCalculation, String> {

    Optional<PackagingLogisticsCostCalculation> findFirstByBomNodeIdOrderByCalculatedAtDesc(String bomNodeId);

    List<PackagingLogisticsCostCalculation> findByBomNodeIdOrderByCalculatedAtDesc(String bomNodeId);

    Optional<PackagingLogisticsCostCalculation> findFirstByProfileIdOrderByCalculatedAtDesc(String profileId);
}
