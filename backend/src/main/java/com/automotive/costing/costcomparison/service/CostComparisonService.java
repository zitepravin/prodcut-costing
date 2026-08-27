package com.automotive.costing.costcomparison.service;

import com.automotive.costing.costcomparison.dto.CostComparisonDtos.QuoteLine;
import com.automotive.costing.costcomparison.dto.CostComparisonDtos.ShouldCostLine;
import com.automotive.costing.costcomparison.dto.CostComparisonDtos.TargetLine;
import com.automotive.costing.costcomparison.dto.CostComparisonDtos.ToolingLine;
import com.automotive.costing.costcomparison.dto.CostComparisonDtos.WorkspaceResponse;
import com.automotive.costing.rfq.entity.QuoteStatus;
import com.automotive.costing.rfq.entity.SupplierQuote;
import com.automotive.costing.rfq.repository.SupplierQuoteRepository;
import com.automotive.costing.shouldcost.entity.ShouldCostBuildUp;
import com.automotive.costing.shouldcost.repository.ShouldCostBuildUpRepository;
import com.automotive.costing.targetcosting.entity.TargetCostRecord;
import com.automotive.costing.targetcosting.entity.TargetCostStatus;
import com.automotive.costing.targetcosting.repository.TargetCostRecordRepository;
import com.automotive.costing.tooling.entity.ToolingCostCalculation;
import com.automotive.costing.tooling.repository.ToolingCostCalculationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CostComparisonService {

    private final ShouldCostBuildUpRepository shouldCostBuildUpRepository;
    private final SupplierQuoteRepository supplierQuoteRepository;
    private final ToolingCostCalculationRepository toolingCostCalculationRepository;
    private final TargetCostRecordRepository targetCostRecordRepository;

    @Transactional(readOnly = true)
    public WorkspaceResponse workspaceForBomNode(String bomNodeId) {
        if (bomNodeId == null || bomNodeId.isBlank()) {
            throw new IllegalArgumentException("bomNodeId is required");
        }

        List<ShouldCostBuildUp> buildUps =
                shouldCostBuildUpRepository.findByBomNodeIdOrderByCalculatedAtDesc(bomNodeId);
        List<ShouldCostLine> shouldLines = buildUps.stream()
                .map(b -> new ShouldCostLine(
                        b.getBuildUpId(),
                        b.getScenarioLabel(),
                        b.getTotalCost(),
                        b.getApprovalStatus() != null ? b.getApprovalStatus().name() : null))
                .toList();

        String lowestScId = buildUps.stream()
                .filter(b -> b.getTotalCost() != null)
                .min(Comparator.comparing(ShouldCostBuildUp::getTotalCost))
                .map(ShouldCostBuildUp::getBuildUpId)
                .orElse(null);
        BigDecimal lowestSc = buildUps.stream()
                .map(ShouldCostBuildUp::getTotalCost)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(null);

        List<SupplierQuote> allQuotes =
                supplierQuoteRepository.findByBomNodeIdOrderByUpdatedAtDesc(bomNodeId);
        List<QuoteLine> quoteLines = allQuotes.stream()
                .map(q -> new QuoteLine(
                        q.getQuoteId(),
                        q.getSupplierId(),
                        q.getUnitPrice(),
                        q.getCurrencyCode(),
                        q.getStatus() != null ? q.getStatus().name() : null,
                        q.getLeadTimeDays()))
                .toList();

        List<SupplierQuote> accepted = allQuotes.stream()
                .filter(q -> q.getStatus() == QuoteStatus.ACCEPTED)
                .toList();
        String lowestQuoteId = accepted.stream()
                .filter(q -> q.getUnitPrice() != null)
                .min(Comparator.comparing(SupplierQuote::getUnitPrice))
                .map(SupplierQuote::getQuoteId)
                .orElse(null);
        BigDecimal lowestQuotePrice = accepted.stream()
                .map(SupplierQuote::getUnitPrice)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(null);

        List<ToolingLine> toolingLines = new ArrayList<>();
        toolingCostCalculationRepository.findFirstByBomNodeIdOrderByCalculatedAtDesc(bomNodeId)
                .ifPresent(t -> toolingLines.add(new ToolingLine(
                        t.getCalculationId(),
                        t.getToolingRecordId(),
                        t.getTotalAmortizedCostPerPiece(),
                        t.isIncludedInPiecePrice())));

        List<TargetCostRecord> targets =
                targetCostRecordRepository.findByBomNodeIdOrderByEffectiveFromDesc(bomNodeId);
        List<TargetLine> targetLines = targets.stream()
                .map(t -> new TargetLine(
                        t.getTargetCostId(),
                        t.getTargetAmount(),
                        t.getCurrencyCode(),
                        t.getStatus() != null ? t.getStatus().name() : null,
                        t.getApprovalStatus() != null ? t.getApprovalStatus().name() : null))
                .toList();
        BigDecimal activeTarget = targets.stream()
                .filter(t -> t.getStatus() == TargetCostStatus.ACTIVE)
                .map(TargetCostRecord::getTargetAmount)
                .findFirst()
                .orElse(null);

        return new WorkspaceResponse(
                bomNodeId,
                shouldLines,
                lowestScId,
                quoteLines,
                lowestQuoteId,
                toolingLines,
                targetLines,
                activeTarget,
                lowestSc,
                lowestQuotePrice);
    }
}
