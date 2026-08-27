package com.automotive.costing.rfq.repository;

import com.automotive.costing.rfq.entity.RfqLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RfqLineRepository extends JpaRepository<RfqLine, String> {
    Optional<RfqLine> findByRfqLineId(String rfqLineId);
    List<RfqLine> findByRfqIdOrderByLineNumberAsc(String rfqId);
    Optional<RfqLine> findByRfqIdAndBomNodeId(String rfqId, String bomNodeId);
}
