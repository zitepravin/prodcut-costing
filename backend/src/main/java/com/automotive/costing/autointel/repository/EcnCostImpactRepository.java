package com.automotive.costing.autointel.repository;
import com.automotive.costing.autointel.entity.EcnCostImpact;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface EcnCostImpactRepository extends JpaRepository<EcnCostImpact, String> {
    List<EcnCostImpact> findByBomNodeIdOrderByCreatedAtDesc(String bomNodeId);
    List<EcnCostImpact> findByEcnNumberOrderByCreatedAtDesc(String ecnNumber);
}
