package com.automotive.costing.erp.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.erp.dto.ErpDtos.BatchDetailResponse;
import com.automotive.costing.erp.dto.ErpDtos.BatchResponse;
import com.automotive.costing.erp.dto.ErpDtos.BomRow;
import com.automotive.costing.erp.dto.ErpDtos.IngestRequest;
import com.automotive.costing.erp.dto.ErpDtos.MaterialPriceRow;
import com.automotive.costing.erp.dto.ErpDtos.PartRow;
import com.automotive.costing.erp.dto.ErpDtos.PartStagingResponse;
import com.automotive.costing.erp.dto.ErpDtos.ValidateResult;
import com.automotive.costing.erp.entity.ErpBatchStatus;
import com.automotive.costing.erp.entity.ErpBomStaging;
import com.automotive.costing.erp.entity.ErpInboundBatch;
import com.automotive.costing.erp.entity.ErpMaterialPriceStaging;
import com.automotive.costing.erp.entity.ErpPartStaging;
import com.automotive.costing.erp.entity.ErpStagingRowStatus;
import com.automotive.costing.erp.repository.ErpBomStagingRepository;
import com.automotive.costing.erp.repository.ErpInboundBatchRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.erp.repository.ErpMaterialPriceStagingRepository;
import com.automotive.costing.erp.repository.ErpPartStagingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * ERP/PLM inbound staging stub: ingest → validate → ready for future promote.
 * Does not write through to live BOM/material masters in this version.
 * All batch state transitions are audited (AUD-01).
 */
@Service
@RequiredArgsConstructor
public class ErpStagingService {

    private static final String ENTITY_TYPE = "ErpInboundBatch";

    private final ErpInboundBatchRepository batchRepository;
    private final TenantAccessGuard tenantAccessGuard;
    private final ErpPartStagingRepository partRepository;
    private final ErpBomStagingRepository bomRepository;
    private final ErpMaterialPriceStagingRepository priceRepository;
    private final AuditService auditService;

    @Transactional
    public BatchResponse ingest(IngestRequest request) {
        List<PartRow> parts = request.parts() != null ? request.parts() : List.of();
        List<BomRow> bomLines = request.bomLines() != null ? request.bomLines() : List.of();
        List<MaterialPriceRow> prices = request.materialPrices() != null ? request.materialPrices() : List.of();
        int total = parts.size() + bomLines.size() + prices.size();
        if (total == 0) {
            throw new BusinessRuleViolationException("Ingest requires at least one staging row", "ERP-01");
        }

        Instant now = Instant.now();
        ErpInboundBatch batch = new ErpInboundBatch();
        batch.setBatchId(UUID.randomUUID().toString());
        batch.setCustomerId(tenantAccessGuard.requireCustomerId());
        batch.setSourceSystem(request.sourceSystem().trim());
        batch.setBatchType(request.batchType());
        batch.setStatus(ErpBatchStatus.RECEIVED);
        batch.setRowCount(total);
        batch.setErrorCount(0);
        batch.setSourceFileName(request.sourceFileName());
        batch.setNotes(request.notes());
        batch.setReceivedAt(now);
        batch.setCreatedAt(now);
        batchRepository.save(batch);
        auditService.recordCreate(ENTITY_TYPE, batch.getBatchId(), toBatchResponse(batch));

        for (PartRow row : parts) {
            ErpPartStaging s = new ErpPartStaging();
            s.setStagingId(UUID.randomUUID().toString());
            s.setBatchId(batch.getBatchId());
            s.setExternalPartId(row.externalPartId());
            s.setPartNumber(row.partNumber());
            s.setRevision(row.revision());
            s.setDescription(row.description());
            s.setNetWeightKg(row.netWeightKg());
            s.setUom(row.uom());
            s.setMaterialCode(row.materialCode());
            s.setMakeBuyCode(row.makeBuyCode());
            s.setPlantCode(row.plantCode());
            s.setProgramCode(row.programCode());
            s.setStatus(ErpStagingRowStatus.PENDING);
            s.setRawPayload(row.rawPayload());
            s.setCreatedAt(now);
            partRepository.save(s);
        }

        for (BomRow row : bomLines) {
            ErpBomStaging s = new ErpBomStaging();
            s.setStagingId(UUID.randomUUID().toString());
            s.setBatchId(batch.getBatchId());
            s.setExternalBomId(row.externalBomId());
            s.setParentPartNumber(row.parentPartNumber());
            s.setParentRevision(row.parentRevision());
            s.setChildPartNumber(row.childPartNumber());
            s.setChildRevision(row.childRevision());
            s.setQuantityPer(row.quantityPer());
            s.setUom(row.uom());
            s.setLineNumber(row.lineNumber());
            s.setPlantCode(row.plantCode());
            s.setProgramCode(row.programCode());
            s.setStatus(ErpStagingRowStatus.PENDING);
            s.setRawPayload(row.rawPayload());
            s.setCreatedAt(now);
            bomRepository.save(s);
        }

        for (MaterialPriceRow row : prices) {
            ErpMaterialPriceStaging s = new ErpMaterialPriceStaging();
            s.setStagingId(UUID.randomUUID().toString());
            s.setBatchId(batch.getBatchId());
            s.setMaterialCode(row.materialCode());
            s.setPlantCode(row.plantCode());
            s.setUnitPrice(row.unitPrice());
            s.setCurrencyCode(row.currencyCode().toUpperCase());
            s.setUom(row.uom());
            s.setEffectiveFrom(row.effectiveFrom());
            s.setEffectiveTo(row.effectiveTo());
            s.setSupplierCode(row.supplierCode());
            s.setStatus(ErpStagingRowStatus.PENDING);
            s.setRawPayload(row.rawPayload());
            s.setCreatedAt(now);
            priceRepository.save(s);
        }

        return toBatchResponse(batch);
    }

    @Transactional
    public ValidateResult validate(String batchId) {
        ErpInboundBatch batch = batchRepository.findByCustomerIdAndBatchId(tenantAccessGuard.requireCustomerId(), batchId)
                .orElseThrow(() -> ResourceNotFoundException.of("ErpInboundBatch", batchId));
        if (batch.getStatus() == ErpBatchStatus.PROMOTED || batch.getStatus() == ErpBatchStatus.CANCELLED) {
            throw new BusinessRuleViolationException(
                    "Cannot validate batch in status " + batch.getStatus(), "ERP-02");
        }

        String statusBeforeValidation = batch.getStatus() != null ? batch.getStatus().name() : null;
        batch.setStatus(ErpBatchStatus.VALIDATING);
        List<String> messages = new ArrayList<>();
        int valid = 0;
        int invalid = 0;

        for (ErpPartStaging p : partRepository.findByBatchIdOrderByCreatedAtAsc(batchId)) {
            if (p.getPartNumber() == null || p.getPartNumber().isBlank()) {
                p.setStatus(ErpStagingRowStatus.INVALID);
                p.setErrorMessage("partNumber is required");
                invalid++;
                messages.add("part " + p.getStagingId() + ": partNumber required");
            } else {
                p.setStatus(ErpStagingRowStatus.VALID);
                p.setErrorMessage(null);
                valid++;
            }
            p.setUpdatedAt(Instant.now());
            partRepository.save(p);
        }

        for (ErpBomStaging b : bomRepository.findByBatchIdOrderByLineNumberAsc(batchId)) {
            if (b.getQuantityPer() == null || b.getQuantityPer().compareTo(BigDecimal.ZERO) <= 0) {
                b.setStatus(ErpStagingRowStatus.INVALID);
                b.setErrorMessage("quantityPer must be > 0");
                invalid++;
                messages.add("bom " + b.getStagingId() + ": quantityPer must be > 0");
            } else {
                b.setStatus(ErpStagingRowStatus.VALID);
                b.setErrorMessage(null);
                valid++;
            }
            b.setUpdatedAt(Instant.now());
            bomRepository.save(b);
        }

        for (ErpMaterialPriceStaging pr : priceRepository.findByBatchIdOrderByCreatedAtAsc(batchId)) {
            if (pr.getUnitPrice() == null || pr.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                pr.setStatus(ErpStagingRowStatus.INVALID);
                pr.setErrorMessage("unitPrice must be >= 0");
                invalid++;
                messages.add("price " + pr.getStagingId() + ": unitPrice invalid");
            } else if (pr.getCurrencyCode() == null || pr.getCurrencyCode().length() != 3) {
                pr.setStatus(ErpStagingRowStatus.INVALID);
                pr.setErrorMessage("currencyCode must be ISO 4217");
                invalid++;
                messages.add("price " + pr.getStagingId() + ": currencyCode invalid");
            } else {
                pr.setStatus(ErpStagingRowStatus.VALID);
                pr.setErrorMessage(null);
                valid++;
            }
            pr.setUpdatedAt(Instant.now());
            priceRepository.save(pr);
        }

        batch.setErrorCount(invalid);
        batch.setStatus(invalid > 0 && valid == 0 ? ErpBatchStatus.FAILED : ErpBatchStatus.READY);
        batch.setProcessedAt(Instant.now());
        batch.setUpdatedAt(Instant.now());
        batchRepository.save(batch);
        auditService.recordUpdate(ENTITY_TYPE, batch.getBatchId(),
                statusBeforeValidation,
                toBatchResponse(batch));

        return new ValidateResult(batchId, batch.getStatus(), valid, invalid, messages);
    }

    /**
     * Cancel a non-promoted batch. Allowed from RECEIVED, VALIDATING, READY, FAILED.
     * PROMOTED batches cannot be cancelled (live data already written).
     */
    @Transactional
    public BatchResponse cancel(String batchId) {
        ErpInboundBatch batch = batchRepository.findByCustomerIdAndBatchId(tenantAccessGuard.requireCustomerId(), batchId)
                .orElseThrow(() -> ResourceNotFoundException.of("ErpInboundBatch", batchId));
        if (batch.getStatus() == ErpBatchStatus.PROMOTED) {
            throw new BusinessRuleViolationException(
                    "Cannot cancel a PROMOTED batch; live masters already updated", "ERP-04");
        }
        if (batch.getStatus() == ErpBatchStatus.CANCELLED) {
            throw new BusinessRuleViolationException(
                    "Batch is already CANCELLED", "ERP-05");
        }
        String before = batch.getStatus().name();
        batch.setStatus(ErpBatchStatus.CANCELLED);
        batch.setUpdatedAt(Instant.now());
        batchRepository.save(batch);
        auditService.recordUpdate(ENTITY_TYPE, batchId, before, toBatchResponse(batch));
        return toBatchResponse(batch);
    }

    /**
     * Re-open a CANCELLED or FAILED batch back to RECEIVED so it can be validated again.
     * Row statuses are reset to PENDING and error messages cleared.
     */
    @Transactional
    public BatchResponse reopen(String batchId) {
        ErpInboundBatch batch = batchRepository.findByCustomerIdAndBatchId(tenantAccessGuard.requireCustomerId(), batchId)
                .orElseThrow(() -> ResourceNotFoundException.of("ErpInboundBatch", batchId));
        if (batch.getStatus() != ErpBatchStatus.CANCELLED && batch.getStatus() != ErpBatchStatus.FAILED) {
            throw new BusinessRuleViolationException(
                    "Only CANCELLED or FAILED batches can be re-opened; current status is "
                            + batch.getStatus(),
                    "ERP-06");
        }
        String before = batch.getStatus().name();
        Instant now = Instant.now();

        for (ErpPartStaging p : partRepository.findByBatchIdOrderByCreatedAtAsc(batchId)) {
            p.setStatus(ErpStagingRowStatus.PENDING);
            p.setErrorMessage(null);
            p.setUpdatedAt(now);
            partRepository.save(p);
        }
        for (ErpBomStaging b : bomRepository.findByBatchIdOrderByLineNumberAsc(batchId)) {
            b.setStatus(ErpStagingRowStatus.PENDING);
            b.setErrorMessage(null);
            b.setUpdatedAt(now);
            bomRepository.save(b);
        }
        for (ErpMaterialPriceStaging pr : priceRepository.findByBatchIdOrderByCreatedAtAsc(batchId)) {
            pr.setStatus(ErpStagingRowStatus.PENDING);
            pr.setErrorMessage(null);
            pr.setUpdatedAt(now);
            priceRepository.save(pr);
        }

        batch.setStatus(ErpBatchStatus.RECEIVED);
        batch.setErrorCount(0);
        batch.setProcessedAt(null);
        batch.setUpdatedAt(now);
        batchRepository.save(batch);
        auditService.recordUpdate(ENTITY_TYPE, batchId, before, toBatchResponse(batch));
        return toBatchResponse(batch);
    }

    @Transactional(readOnly = true)
    public List<BatchResponse> listBatches() {
        return batchRepository.findByCustomerIdOrderByReceivedAtDesc(tenantAccessGuard.requireCustomerId()).stream()
                .map(this::toBatchResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BatchDetailResponse getBatch(String batchId) {
        ErpInboundBatch batch = batchRepository.findByCustomerIdAndBatchId(tenantAccessGuard.requireCustomerId(), batchId)
                .orElseThrow(() -> ResourceNotFoundException.of("ErpInboundBatch", batchId));
        List<PartStagingResponse> parts = partRepository.findByBatchIdOrderByCreatedAtAsc(batchId).stream()
                .map(p -> new PartStagingResponse(
                        p.getStagingId(), p.getBatchId(), p.getPartNumber(), p.getRevision(),
                        p.getNetWeightKg(), p.getStatus(), p.getErrorMessage()))
                .toList();
        int bomCount = bomRepository.findByBatchIdOrderByLineNumberAsc(batchId).size();
        int priceCount = priceRepository.findByBatchIdOrderByCreatedAtAsc(batchId).size();
        return new BatchDetailResponse(toBatchResponse(batch), parts, bomCount, priceCount);
    }

    private BatchResponse toBatchResponse(ErpInboundBatch b) {
        return new BatchResponse(
                b.getBatchId(),
                b.getSourceSystem(),
                b.getBatchType(),
                b.getStatus(),
                b.getRowCount(),
                b.getErrorCount(),
                b.getSourceFileName(),
                b.getReceivedAt(),
                b.getProcessedAt());
    }
}
