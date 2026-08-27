package com.automotive.costing.rfq.repository;
import com.automotive.costing.rfq.entity.RfqClarificationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RfqClarificationRequestRepository extends JpaRepository<RfqClarificationRequest, String> {
    List<RfqClarificationRequest> findByRfqIdOrderByCreatedAtDesc(String rfqId);
    List<RfqClarificationRequest> findByRfqIdAndSupplierIdOrderByCreatedAtDesc(String rfqId, String supplierId);
}
