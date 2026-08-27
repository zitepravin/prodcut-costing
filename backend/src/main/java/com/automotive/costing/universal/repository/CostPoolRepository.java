package com.automotive.costing.universal.repository;
import com.automotive.costing.universal.entity.CostPool;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; import java.util.Optional;
public interface CostPoolRepository extends JpaRepository<CostPool, String> {
  List<CostPool> findByCustomerIdOrderByPoolCodeAsc(String customerId);
  Optional<CostPool> findByCustomerIdAndPoolCode(String customerId, String poolCode);
  Optional<CostPool> findByCustomerIdAndCostPoolId(String customerId, String costPoolId);
  boolean existsByCustomerIdAndPoolCode(String customerId, String poolCode);
}
