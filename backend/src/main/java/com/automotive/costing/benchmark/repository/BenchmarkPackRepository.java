package com.automotive.costing.benchmark.repository;

import com.automotive.costing.benchmark.entity.BenchmarkPack;
import com.automotive.costing.benchmark.entity.BenchmarkPackStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BenchmarkPackRepository extends JpaRepository<BenchmarkPack, String> {
    List<BenchmarkPack> findAllByOrderByPackCodeAscVersionLabelAsc();
    List<BenchmarkPack> findByStatusOrderByPackCodeAsc(BenchmarkPackStatus status);
    Optional<BenchmarkPack> findByPackCodeAndVersionLabel(String packCode, String versionLabel);
}
