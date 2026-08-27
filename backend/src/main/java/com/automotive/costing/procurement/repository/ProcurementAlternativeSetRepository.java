package com.automotive.costing.procurement.repository;

import com.automotive.costing.procurement.entity.ProcurementAlternativeSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProcurementAlternativeSetRepository extends JpaRepository<ProcurementAlternativeSet, String> {
    List<ProcurementAlternativeSet> findByBomNodeIdOrderByCreatedAtDesc(String bomNodeId);
}
