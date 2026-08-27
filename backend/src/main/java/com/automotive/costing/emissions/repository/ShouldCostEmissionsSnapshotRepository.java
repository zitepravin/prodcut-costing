package com.automotive.costing.emissions.repository;

import com.automotive.costing.emissions.entity.ShouldCostEmissionsSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShouldCostEmissionsSnapshotRepository extends JpaRepository<ShouldCostEmissionsSnapshot, String> {
    Optional<ShouldCostEmissionsSnapshot> findByBuildUpId(String buildUpId);
}
