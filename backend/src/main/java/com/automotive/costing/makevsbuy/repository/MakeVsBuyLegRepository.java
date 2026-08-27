package com.automotive.costing.makevsbuy.repository;

import com.automotive.costing.makevsbuy.entity.MakeVsBuyLeg;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MakeVsBuyLegRepository extends JpaRepository<MakeVsBuyLeg, MakeVsBuyLeg.Pk> {
    List<MakeVsBuyLeg> findBySimulationIdOrderByLegSeqAsc(String simulationId);
    void deleteBySimulationId(String simulationId);
}
