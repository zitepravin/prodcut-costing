package com.automotive.costing.shouldcost.repository;
import com.automotive.costing.shouldcost.entity.CostComponentDefinition;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface CostComponentDefinitionRepository extends JpaRepository<CostComponentDefinition, String> {
  @Query("SELECT c FROM CostComponentDefinition c WHERE c.active = true AND (c.tenantId IS NULL OR c.tenantId = :tenantId) ORDER BY c.sequenceNo ASC")
  List<CostComponentDefinition> findActiveForTenant(@Param("tenantId") String tenantId);
}
