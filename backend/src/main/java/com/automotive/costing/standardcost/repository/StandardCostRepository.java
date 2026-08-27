package com.automotive.costing.standardcost.repository;

import com.automotive.costing.standardcost.entity.StandardCost;
import com.automotive.costing.standardcost.entity.StandardCostStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StandardCostRepository extends JpaRepository<StandardCost, String> {

    List<StandardCost> findByBomNodeIdOrderByVersionNoDesc(String bomNodeId);

    Optional<StandardCost> findFirstByBomNodeIdAndStatusOrderByVersionNoDesc(
            String bomNodeId, StandardCostStatus status);

    @Query("SELECT COALESCE(MAX(s.versionNo), 0) FROM StandardCost s WHERE s.bomNodeId = :bomNodeId")
    int maxVersionNo(@Param("bomNodeId") String bomNodeId);

    List<StandardCost> findByBomNodeIdAndStatusIn(String bomNodeId, List<StandardCostStatus> statuses);
}
