package com.automotive.costing.benchmark.repository;

import com.automotive.costing.benchmark.entity.BenchmarkCategory;
import com.automotive.costing.benchmark.entity.BenchmarkRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BenchmarkRateRepository extends JpaRepository<BenchmarkRate, String> {
    List<BenchmarkRate> findByPackIdOrderByCategoryAscRateCodeAsc(String packId);
    List<BenchmarkRate> findByPackIdAndCategoryOrderByRateCodeAsc(String packId, BenchmarkCategory category);
    long countByPackId(String packId);
}
