package com.automotive.costing.procurement.service;

import com.automotive.costing.bom.entity.BomNode;
import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.mixedcost.entity.MixedCostVersion;
import com.automotive.costing.mixedcost.repository.MixedCostVersionRepository;
import com.automotive.costing.procurement.dto.ProcurementDtos.*;
import com.automotive.costing.procurement.entity.*;
import com.automotive.costing.procurement.repository.ProcurementAlternativeLineRepository;
import com.automotive.costing.procurement.repository.ProcurementAlternativeSetRepository;
import com.automotive.costing.rfq.entity.QuoteStatus;
import com.automotive.costing.rfq.entity.SupplierQuote;
import com.automotive.costing.rfq.repository.SupplierQuoteRepository;
import com.automotive.costing.shouldcost.entity.ShouldCostBuildUp;
import com.automotive.costing.shouldcost.repository.ShouldCostBuildUpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * B6 Procurement Alternatives — first-class MAKE / BUY / SUBCONTRACT / EXTERNAL / MIXED set.
 * Extends make-vs-buy; does not auto-award business. Human decision required.
 */
@Service
@RequiredArgsConstructor
public class ProcurementAlternativeService {

    private static final int SCALE = 6;

    private final ProcurementAlternativeSetRepository setRepository;
    private final ProcurementAlternativeLineRepository lineRepository;
    private final BomNodeRepository bomNodeRepository;
    private final ShouldCostBuildUpRepository shouldCostBuildUpRepository;
    private final SupplierQuoteRepository quoteRepository;
    private final MixedCostVersionRepository mixedCostVersionRepository;

    @Transactional(readOnly = true)
    public List<SetResponse> listByBomNode(String bomNodeId) {
        return setRepository.findByBomNodeIdOrderByCreatedAtDesc(bomNodeId).stream()
                .map(this::toSet)
                .toList();
    }

    @Transactional(readOnly = true)
    public SetResponse get(String setId) {
        return toSet(requireSet(setId));
    }

    /**
     * Build alternative set from live authorized data for the BOM node, rank by unit cost,
     * produce recommendation draft (lowest eligible cost — never automatic award).
     */
    @Transactional
    public SetResponse evaluate(EvaluateRequest req) {
        BomNode node = bomNodeRepository.findByBomNodeId(req.bomNodeId().trim())
                .orElseThrow(() -> ResourceNotFoundException.of("BomNode", req.bomNodeId()));

        ProcurementAlternativeSet set = new ProcurementAlternativeSet();
        set.setSetId(UUID.randomUUID().toString());
        set.setBomNodeId(node.getBomNodeId());
        set.setProgramId(req.programId() != null && !req.programId().isBlank()
                ? req.programId().trim() : node.getProgramId());
        set.setLabel(req.label() != null && !req.label().isBlank()
                ? req.label().trim() : "Procurement alternatives " + Instant.now());
        set.setStatus(ProcurementSetStatus.DRAFT);
        setRepository.save(set);

        List<ProcurementAlternativeLine> lines = new ArrayList<>();
        int seq = 10;

        // MAKE from latest should-cost
        List<ShouldCostBuildUp> scList = shouldCostBuildUpRepository
                .findByBomNodeIdOrderByCalculatedAtDesc(node.getBomNodeId());
        if (!scList.isEmpty()) {
            ShouldCostBuildUp sc = scList.get(0);
            lines.add(line(set.getSetId(), seq, AlternativeType.MAKE,
                    "SHOULD_COST", sc.getBuildUpId(), null, null,
                    sc.getTotalCost(), "USD",
                    "Internal should-cost (MAKE)"));
            seq += 10;
        } else {
            lines.add(missing(set.getSetId(), seq, AlternativeType.MAKE,
                    "No should-cost build-up for BOM node"));
            seq += 10;
        }

        // BUY from accepted quotes
        List<SupplierQuote> accepted = quoteRepository
                .findByBomNodeIdAndStatusOrderByUpdatedAtDesc(node.getBomNodeId(), QuoteStatus.ACCEPTED);
        if (accepted.isEmpty()) {
            // also surface any quotes for visibility
            List<SupplierQuote> any = quoteRepository.findByBomNodeIdOrderByUpdatedAtDesc(node.getBomNodeId());
            for (SupplierQuote q : any) {
                lines.add(line(set.getSetId(), seq, AlternativeType.BUY,
                        "SUPPLIER_QUOTE", q.getQuoteId(), q.getSupplierId(), null,
                        q.getUnitPrice(), q.getCurrencyCode(),
                        "Quote status=" + q.getStatus() + " (not ACCEPTED — eligibility UNKNOWN)"));
                seq += 10;
            }
            if (any.isEmpty()) {
                lines.add(missing(set.getSetId(), seq, AlternativeType.BUY, "No supplier quotes for BOM node"));
                seq += 10;
            }
        } else {
            for (SupplierQuote q : accepted) {
                ProcurementAlternativeLine l = line(set.getSetId(), seq, AlternativeType.BUY,
                        "SUPPLIER_QUOTE", q.getQuoteId(), q.getSupplierId(), null,
                        q.getUnitPrice(), q.getCurrencyCode(),
                        "Accepted supplier quote");
                l.setEligibility("ELIGIBLE");
                lines.add(l);
                seq += 10;
            }
        }

        // SUBCONTRACT (manual if provided, else marker)
        if (req.subcontractUnitCost() != null) {
            lines.add(line(set.getSetId(), seq, AlternativeType.SUBCONTRACT,
                    "MANUAL", null, null, req.subcontractSupplierName(),
                    req.subcontractUnitCost(), "USD",
                    "Subcontract unit cost provided for evaluation"));
        } else {
            lines.add(missing(set.getSetId(), seq, AlternativeType.SUBCONTRACT,
                    "No subcontract cost entered — provide subcontractUnitCost to evaluate"));
        }
        seq += 10;

        // EXTERNAL_PROCESS
        if (req.externalProcessUnitCost() != null) {
            lines.add(line(set.getSetId(), seq, AlternativeType.EXTERNAL_PROCESS,
                    "MANUAL", null, null, req.externalSupplierName(),
                    req.externalProcessUnitCost(), "USD",
                    "External process unit cost provided for evaluation"));
        } else {
            lines.add(missing(set.getSetId(), seq, AlternativeType.EXTERNAL_PROCESS,
                    "No external-process cost entered — provide externalProcessUnitCost to evaluate"));
        }
        seq += 10;

        // MIXED
        List<MixedCostVersion> mixed = mixedCostVersionRepository
                .findByBomNodeIdOrderByCreatedAtDesc(node.getBomNodeId());
        if (!mixed.isEmpty()) {
            MixedCostVersion m = mixed.get(0);
            BigDecimal unit = m.getBlendedUnitTotal();
            ProcurementAlternativeLine mixedLine = line(set.getSetId(), seq, AlternativeType.MIXED,
                    "MIXED_COST", m.getMixedCostId(),
                    null, null, unit, m.getReportingCurrency() != null ? m.getReportingCurrency() : "USD",
                    unit != null ? "Latest mixed-cost version" : "Mixed cost present — unit cost DATA_NOT_AVAILABLE");
            if (unit == null) {
                mixedLine.setDataStatus("DATA_NOT_AVAILABLE");
            }
            lines.add(mixedLine);
        } else {
            lines.add(missing(set.getSetId(), seq, AlternativeType.MIXED, "No mixed-cost version for BOM node"));
        }

        // Rank by unit cost among lines with cost
        List<ProcurementAlternativeLine> ranked = lines.stream()
                .filter(l -> l.getUnitCost() != null && "OK".equals(l.getDataStatus()))
                .sorted(Comparator.comparing(ProcurementAlternativeLine::getUnitCost))
                .toList();
        int rank = 1;
        for (ProcurementAlternativeLine l : ranked) {
            l.setRankNo(rank++);
        }
        lineRepository.saveAll(lines);

        // Recommendation: lowest unit cost among OK — human must still decide
        if (!ranked.isEmpty()) {
            ProcurementAlternativeLine best = ranked.get(0);
            set.setRecommendationCode(best.getAlternativeType().name());
            set.setRecommendationSummary(
                    "AI/system draft: lowest unit cost among available alternatives is "
                            + best.getAlternativeType().name()
                            + " at " + best.getUnitCost().toPlainString()
                            + (best.getSourceRefId() != null ? " (ref " + best.getSourceRefId() + ")" : "")
                            + ". This is NOT an automatic award — human decision required."
            );
        } else {
            set.setRecommendationCode("INSUFFICIENT_DATA");
            set.setRecommendationSummary(
                    "No alternative has a complete unit cost. Supply should-cost, quotes, or manual SUBCONTRACT/EXTERNAL costs.");
        }
        set.setStatus(ProcurementSetStatus.EVALUATED);
        setRepository.save(set);
        return toSet(set);
    }

    @Transactional
    public SetResponse decide(String setId, DecisionRequest req) {
        ProcurementAlternativeSet set = requireSet(setId);
        if (set.getStatus() == ProcurementSetStatus.DECIDED) {
            throw new BusinessRuleViolationException("Set already decided", "PA_ALREADY_DECIDED");
        }
        set.setDecisionCode(req.decisionCode().trim());
        set.setDecisionReason(req.decisionReason());
        set.setDecidedAt(Instant.now());
        set.setStatus(ProcurementSetStatus.DECIDED);
        // Human decision only — does not change BOM make/buy or award PO
        setRepository.save(set);
        return toSet(set);
    }

    private ProcurementAlternativeLine line(
            String setId, int seq, AlternativeType type, String refType, String refId,
            String supplierId, String supplierName, BigDecimal unitCost, String currency, String notes) {
        ProcurementAlternativeLine l = new ProcurementAlternativeLine();
        l.setLineId(UUID.randomUUID().toString());
        l.setSetId(setId);
        l.setSequenceNo(seq);
        l.setAlternativeType(type);
        l.setSourceRefType(refType);
        l.setSourceRefId(refId);
        l.setSupplierId(supplierId);
        l.setSupplierName(supplierName);
        l.setUnitCost(unitCost != null ? unitCost.setScale(SCALE, RoundingMode.HALF_UP) : null);
        l.setCurrencyCode(currency);
        l.setTcoStatus("DATA_NOT_AVAILABLE");
        l.setEligibility(unitCost != null ? "ELIGIBLE" : "UNKNOWN");
        l.setDataStatus(unitCost != null ? "OK" : "DATA_NOT_AVAILABLE");
        l.setNotes(notes);
        return l;
    }

    private ProcurementAlternativeLine missing(String setId, int seq, AlternativeType type, String notes) {
        return line(setId, seq, type, null, null, null, null, null, null, notes);
    }

    private ProcurementAlternativeSet requireSet(String id) {
        return setRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("ProcurementAlternativeSet", id));
    }

    private SetResponse toSet(ProcurementAlternativeSet set) {
        List<AlternativeLineResponse> lines = lineRepository.findBySetIdOrderBySequenceNoAsc(set.getSetId())
                .stream()
                .map(l -> new AlternativeLineResponse(
                        l.getLineId(), l.getSequenceNo(), l.getAlternativeType(),
                        l.getSourceRefType(), l.getSourceRefId(), l.getSupplierId(), l.getSupplierName(),
                        l.getUnitCost(), l.getCurrencyCode(), l.getTcoUnit(), l.getTcoStatus(),
                        l.getEligibility(), l.getRankNo(), l.getDataStatus(), l.getNotes()))
                .toList();
        return new SetResponse(
                set.getSetId(), set.getBomNodeId(), set.getProgramId(), set.getLabel(), set.getStatus(),
                set.getRecommendationCode(), set.getRecommendationSummary(),
                set.getDecisionCode(), set.getDecisionReason(), set.getDecidedAt(), lines
        );
    }
}
