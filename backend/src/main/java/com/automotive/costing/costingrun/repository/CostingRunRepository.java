package com.automotive.costing.costingrun.repository;
import com.automotive.costing.costingrun.entity.CostingRun;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface CostingRunRepository extends JpaRepository<CostingRun, String> {
    Optional<CostingRun> findByRunCode(String runCode);
    List<CostingRun> findAllByOrderByCreatedAtDesc();
}
