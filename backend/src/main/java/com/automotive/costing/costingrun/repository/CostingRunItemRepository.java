package com.automotive.costing.costingrun.repository;
import com.automotive.costing.costingrun.entity.CostingRunItem;
import com.automotive.costing.costingrun.entity.CostingRunItemStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CostingRunItemRepository extends JpaRepository<CostingRunItem, String> {
    List<CostingRunItem> findByRunIdOrderByPartNumberAsc(String runId);
    List<CostingRunItem> findByRunIdAndStatus(String runId, CostingRunItemStatus status);
}
