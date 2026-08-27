package com.automotive.costing.universal.repository;
import com.automotive.costing.universal.entity.CostDriver;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; import java.util.Optional;
public interface CostDriverRepository extends JpaRepository<CostDriver, String> {
  List<CostDriver> findByCustomerIdOrderByDriverCodeAsc(String customerId);
  Optional<CostDriver> findByCustomerIdAndDriverCode(String customerId, String driverCode);
  Optional<CostDriver> findByCustomerIdAndCostDriverId(String customerId, String costDriverId);
  boolean existsByCustomerIdAndDriverCode(String customerId, String driverCode);
}
