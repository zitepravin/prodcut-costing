package com.automotive.costing.procurement.repository;

import com.automotive.costing.procurement.entity.ProcurementAlternativeLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProcurementAlternativeLineRepository extends JpaRepository<ProcurementAlternativeLine, String> {
    List<ProcurementAlternativeLine> findBySetIdOrderBySequenceNoAsc(String setId);
    void deleteBySetId(String setId);
}
