package com.automotive.costing.shouldcost.repository;

import com.automotive.costing.shouldcost.entity.ParametricCostBenchmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParametricCostBenchmarkRepository extends JpaRepository<ParametricCostBenchmark, String> {

    Optional<ParametricCostBenchmark> findFirstByMaterialFamilyAndProcessFamilyAndRegionAndActiveTrue(
            String materialFamily, String processFamily, String region);
}
