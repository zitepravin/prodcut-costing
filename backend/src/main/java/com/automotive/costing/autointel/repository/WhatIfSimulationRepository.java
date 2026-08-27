package com.automotive.costing.autointel.repository;
import com.automotive.costing.autointel.entity.WhatIfSimulation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface WhatIfSimulationRepository extends JpaRepository<WhatIfSimulation, String> {
    List<WhatIfSimulation> findByBomNodeIdOrderByCreatedAtDesc(String bomNodeId);
}
