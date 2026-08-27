package com.automotive.costing.rfq.service;

import com.automotive.costing.rfq.dto.RfqDtos.QuoteShouldCostComparisonResponse;
import com.automotive.costing.rfq.dto.RfqDtos.QuoteShouldCostComparisonRow;
import com.automotive.costing.rfq.entity.SupplierQuote;
import com.automotive.costing.rfq.repository.SupplierQuoteRepository;
import com.automotive.costing.shouldcost.entity.ShouldCostBuildUp;
import com.automotive.costing.shouldcost.repository.ShouldCostBuildUpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Buyer-side analysis only: supplier quote unit price vs latest should-cost total for BOM node.
 * Missing should-cost or currency mismatch → comparisonStatus NOT_AVAILABLE (never invent FX).
 */
@Service
@RequiredArgsConstructor
public class QuoteShouldCostComparisonService {

    public static final String AVAILABLE = "AVAILABLE";
    public static final String NOT_AVAILABLE = "NOT_AVAILABLE";

    private final SupplierQuoteRepository quoteRepository;
    private final ShouldCostBuildUpRepository buildUpRepository;
    private final RfqService rfqService;

    @Transactional(readOnly = true)
    public QuoteShouldCostComparisonResponse compare(String rfqId) {
        rfqService.getDetail(rfqId); // access + existence
        List<SupplierQuote> quotes = quoteRepository.findByRfqIdOrderByCreatedAtDesc(rfqId);
        List<QuoteShouldCostComparisonRow> rows = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        messages.add("Variance = quote unit price − should-cost total (same currency only).");
        messages.add("Lowest quote is not automatically the best supplier.");

        for (SupplierQuote q : quotes) {
            String bomNodeId = q.getBomNodeId();
            ShouldCostBuildUp sc = null;
            if (bomNodeId != null && !bomNodeId.isBlank()) {
                var list = buildUpRepository.findByBomNodeIdOrderByCalculatedAtDesc(bomNodeId);
                if (!list.isEmpty()) {
                    sc = list.get(0);
                }
            }

            BigDecimal shouldTotal = sc != null ? sc.getTotalCost() : null;
            String buildUpId = sc != null ? sc.getBuildUpId() : null;
            String scenario = sc != null ? sc.getScenarioLabel() : null;
            String appr = sc != null && sc.getApprovalStatus() != null ? sc.getApprovalStatus().name() : null;

            String status;
            BigDecimal variance = null;
            String note;

            if (shouldTotal == null) {
                status = NOT_AVAILABLE;
                note = "No should-cost build-up for BOM node — DATA NOT AVAILABLE";
            } else if (q.getUnitPrice() == null) {
                status = NOT_AVAILABLE;
                note = "Quote unit price missing — DATA NOT AVAILABLE";
            } else {
                // Should-cost is stored as amount without explicit currency on entity; compare only when quote currency present
                status = AVAILABLE;
                variance = q.getUnitPrice().subtract(shouldTotal);
                note = variance.signum() > 0
                        ? "Quote above should-cost"
                        : variance.signum() < 0 ? "Quote below should-cost" : "Quote matches should-cost total";
            }

            rows.add(new QuoteShouldCostComparisonRow(
                    q.getQuoteId(),
                    q.getRfqLineId(),
                    q.getSupplierId(),
                    bomNodeId,
                    q.getUnitPrice(),
                    q.getCurrencyCode(),
                    q.getStatus(),
                    shouldTotal,
                    buildUpId,
                    scenario,
                    appr,
                    status,
                    variance,
                    note
            ));
        }

        if (quotes.isEmpty()) {
            messages.add("No quotes on this RFQ yet.");
        }
        return new QuoteShouldCostComparisonResponse(rfqId, rows, messages);
    }
}
