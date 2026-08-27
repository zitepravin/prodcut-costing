package com.automotive.costing.universal.repository;
import com.automotive.costing.universal.entity.UniversalCostCalculation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; import java.util.Optional;
public interface UniversalCostCalculationRepository extends JpaRepository<UniversalCostCalculation, String> {
  List<UniversalCostCalculation> findByCustomerIdOrderByCreatedAtDesc(String customerId);
  List<UniversalCostCalculation> findByCustomerIdAndCostObjectIdOrderByCreatedAtDesc(String customerId, String costObjectId);
  Optional<UniversalCostCalculation> findByCustomerIdAndCalculationId(String customerId, String calculationId);
}
