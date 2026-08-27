package com.automotive.costing.rfq.repository;

import com.automotive.costing.rfq.entity.QuoteStatus;
import com.automotive.costing.rfq.entity.SupplierQuote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierQuoteRepository extends JpaRepository<SupplierQuote, String> {

    Optional<SupplierQuote> findByCustomerIdAndQuoteId(String customerId, String quoteId);

    List<SupplierQuote> findByCustomerIdAndRfqIdOrderByCreatedAtDesc(String customerId, String rfqId);

    Optional<SupplierQuote> findByQuoteId(String quoteId);
    List<SupplierQuote> findByRfqIdOrderByCreatedAtDesc(String rfqId);
    List<SupplierQuote> findByRfqLineIdOrderByCreatedAtDesc(String rfqLineId);
    List<SupplierQuote> findByBomNodeIdAndStatusOrderByUpdatedAtDesc(String bomNodeId, QuoteStatus status);
    Optional<SupplierQuote> findFirstByBomNodeIdAndStatusOrderByUpdatedAtDesc(String bomNodeId, QuoteStatus status);
    List<SupplierQuote> findByBomNodeIdOrderByUpdatedAtDesc(String bomNodeId);
}
