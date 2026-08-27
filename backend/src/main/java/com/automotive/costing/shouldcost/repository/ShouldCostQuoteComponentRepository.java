package com.automotive.costing.shouldcost.repository;
import com.automotive.costing.shouldcost.entity.ShouldCostQuoteComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ShouldCostQuoteComponentRepository extends JpaRepository<ShouldCostQuoteComponent, String> {
  List<ShouldCostQuoteComponent> findByBuildUpId(String buildUpId);
}
