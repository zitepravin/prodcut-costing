package com.automotive.costing.mixedcost.repository;

import com.automotive.costing.mixedcost.entity.MixedCostVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MixedCostVersionRepository extends JpaRepository<MixedCostVersion, String> {

    List<MixedCostVersion> findByBomNodeIdOrderByCreatedAtDesc(String bomNodeId);

    List<MixedCostVersion> findByProgramIdOrderByCreatedAtDesc(String programId);
}
