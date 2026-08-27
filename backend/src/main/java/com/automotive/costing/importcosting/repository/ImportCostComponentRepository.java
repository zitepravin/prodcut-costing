package com.automotive.costing.importcosting.repository;
import com.automotive.costing.importcosting.entity.ImportCostComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ImportCostComponentRepository extends JpaRepository<ImportCostComponent, String> {
  List<ImportCostComponent> findByCustomerIdAndCalculationIdOrderBySortOrderAsc(String customerId, String calculationId);
  void deleteByCalculationId(String calculationId);
}
