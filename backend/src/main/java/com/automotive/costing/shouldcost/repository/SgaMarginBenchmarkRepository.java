package com.automotive.costing.shouldcost.repository;

import com.automotive.costing.shouldcost.entity.SgaMarginBenchmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SgaMarginBenchmarkRepository extends JpaRepository<SgaMarginBenchmark, String> {

    Optional<SgaMarginBenchmark> findFirstByCommodityFamilyAndRegionAndActiveTrue(
            String commodityFamily, String region);
}
