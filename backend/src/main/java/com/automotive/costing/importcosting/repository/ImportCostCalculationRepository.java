package com.automotive.costing.importcosting.repository;
import com.automotive.costing.importcosting.entity.ImportCostCalculation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ImportCostCalculationRepository extends JpaRepository<ImportCostCalculation, String> {
  Optional<ImportCostCalculation> findByCustomerIdAndCalculationId(String customerId, String calculationId);
  List<ImportCostCalculation> findByCustomerIdOrderByCreatedAtDesc(String customerId);
}
