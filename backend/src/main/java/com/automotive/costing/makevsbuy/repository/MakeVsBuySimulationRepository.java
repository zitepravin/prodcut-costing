package com.automotive.costing.makevsbuy.repository;

import com.automotive.costing.makevsbuy.entity.MakeVsBuySimulation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MakeVsBuySimulationRepository extends JpaRepository<MakeVsBuySimulation, String> {
    List<MakeVsBuySimulation> findByBomNodeIdOrderByCreatedAtDesc(String bomNodeId);
}
