package com.automotive.costing.financial.service;

import com.automotive.costing.bom.entity.BomNode;
import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.financial.dto.FinancialDtos.ActualCostResponse;
import com.automotive.costing.financial.dto.FinancialDtos.ImportActualRequest;
import com.automotive.costing.financial.entity.ActualCostRecord;
import com.automotive.costing.financial.repository.ActualCostRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * C2 — Actual cost is import-only. Never invent actuals from planned/standard/should.
 */
@Service
@RequiredArgsConstructor
public class ActualCostService {

    private static final int SCALE = 6;

    private final ActualCostRecordRepository repository;
    private final BomNodeRepository bomNodeRepository;
    private final TenantAccessGuard tenantAccessGuard;

    @Transactional(readOnly = true)
    public List<ActualCostResponse> list(String bomNodeId) {
        String customerId = tenantAccessGuard.requireCustomerId();
        return repository.findByCustomerIdAndBomNodeIdOrderByImportedAtDesc(customerId, bomNodeId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public ActualCostResponse importActual(ImportActualRequest req) {
        BomNode node = bomNodeRepository.findByBomNodeId(req.bomNodeId().trim())
                .orElseThrow(() -> ResourceNotFoundException.of("BomNode", req.bomNodeId()));
        if (req.unitCost().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleViolationException("Actual unit cost cannot be negative", "ACT_NEG");
        }

        ActualCostRecord r = new ActualCostRecord();
        r.setActualCostId(UUID.randomUUID().toString());
        r.setCustomerId(tenantAccessGuard.requireCustomerId());
        r.setBomNodeId(node.getBomNodeId());
        r.setProgramId(req.programId() != null && !req.programId().isBlank()
                ? req.programId().trim() : node.getProgramId());
        r.setSourceSystem(req.sourceSystem() == null || req.sourceSystem().isBlank()
                ? "IMPORT" : req.sourceSystem().trim().toUpperCase());
        r.setSourceRef(trimToNull(req.sourceRef()));
        r.setPeriodCode(trimToNull(req.periodCode()));
        r.setQuantity(req.quantity());
        r.setUnitCost(req.unitCost().setScale(SCALE, RoundingMode.HALF_UP));
        if (req.totalCost() != null) {
            r.setTotalCost(req.totalCost().setScale(SCALE, RoundingMode.HALF_UP));
        } else if (req.quantity() != null) {
            r.setTotalCost(req.unitCost().multiply(req.quantity()).setScale(SCALE, RoundingMode.HALF_UP));
        }
        r.setCurrencyCode(req.currencyCode() == null || req.currencyCode().isBlank()
                ? "USD" : req.currencyCode().trim().toUpperCase());
        r.setMaterialCost(req.materialCost());
        r.setProcessCost(req.processCost());
        r.setOverheadCost(req.overheadCost());
        r.setOtherCost(req.otherCost());
        r.setPostingDate(req.postingDate());
        r.setImportedAt(Instant.now());
        r.setNotes(trimToNull(req.notes()));
        r.setCreatedAt(Instant.now());
        return toResponse(repository.save(r));
    }

    private ActualCostResponse toResponse(ActualCostRecord r) {
        return new ActualCostResponse(
                r.getActualCostId(), r.getBomNodeId(), r.getProgramId(), r.getSourceSystem(),
                r.getSourceRef(), r.getPeriodCode(), r.getQuantity(), r.getUnitCost(), r.getTotalCost(),
                r.getCurrencyCode(), r.getMaterialCost(), r.getProcessCost(), r.getOverheadCost(),
                r.getOtherCost(), r.getPostingDate(), r.getImportedAt(), r.getNotes()
        );
    }

    private static String trimToNull(String s) {
        if (s == null || s.isBlank()) return null;
        return s.trim();
    }
}
