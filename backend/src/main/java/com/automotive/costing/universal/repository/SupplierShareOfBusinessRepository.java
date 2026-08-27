package com.automotive.costing.universal.repository;
import com.automotive.costing.universal.entity.SupplierShareOfBusiness;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; import org.springframework.data.repository.query.Param;
import java.time.LocalDate; import java.util.List; import java.util.Optional;
public interface SupplierShareOfBusinessRepository extends JpaRepository<SupplierShareOfBusiness, String> {
  List<SupplierShareOfBusiness> findByCustomerIdOrderByAllocationGroupCodeAscPriorityAsc(String customerId);
  Optional<SupplierShareOfBusiness> findByCustomerIdAndSobId(String customerId, String sobId);
  @Query("""
    SELECT s FROM SupplierShareOfBusiness s WHERE s.customerId = :customerId AND s.allocationGroupCode = :groupCode
      AND s.status = 'ACTIVE' AND s.effectiveFrom <= :asOf
      AND (s.effectiveTo IS NULL OR s.effectiveTo >= :asOf) ORDER BY s.priority ASC""")
  List<SupplierShareOfBusiness> findActiveForGroup(@Param("customerId") String customerId, @Param("groupCode") String groupCode, @Param("asOf") LocalDate asOf);
}
