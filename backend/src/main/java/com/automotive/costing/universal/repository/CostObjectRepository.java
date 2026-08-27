package com.automotive.costing.universal.repository;
import com.automotive.costing.universal.entity.CostObject;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; import java.util.Optional;
public interface CostObjectRepository extends JpaRepository<CostObject, String> {
  List<CostObject> findByCustomerIdOrderByObjectCodeAsc(String customerId);
  Optional<CostObject> findByCustomerIdAndObjectCode(String customerId, String objectCode);
  Optional<CostObject> findByCustomerIdAndCostObjectId(String customerId, String costObjectId);
  boolean existsByCustomerIdAndObjectCode(String customerId, String objectCode);
}
