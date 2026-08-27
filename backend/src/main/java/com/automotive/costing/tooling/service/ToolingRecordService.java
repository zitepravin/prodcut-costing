package com.automotive.costing.tooling.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.tooling.dto.ToolingRecordDtos.*;
import com.automotive.costing.tooling.entity.ToolingAssetStatus;
import com.automotive.costing.tooling.entity.ToolingRecord;
import com.automotive.costing.tooling.repository.ToolingRecordRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * M06 Tooling, Die &amp; Fixture Investment Costing - record management (TL-01, TL-03,
 * TL-05, TL-07). Cost calculation itself is {@code ToolingCostService}'s job, mirroring
 * the routing-vs-cost-engine split M04 established between {@code ProcessRoutingService}
 * and {@code ProcessCostService}.
 */
@Service
@RequiredArgsConstructor
public class ToolingRecordService {

    private static final String ENTITY_TYPE = "ToolingRecord";

    private final ToolingRecordRepository repository;
    private final TenantAccessGuard tenantAccessGuard;
    private final AuditService auditService;

    /** TL-01: parametric tooling record creation (>=6 tooling types via ToolingType). */
    @Transactional
    public Response create(CreateRequest request) {
        ToolingRecord entity = new ToolingRecord();
        entity.setCustomerId(tenantAccessGuard.requireCustomerId());
        entity.setToolingRecordId(UUID.randomUUID().toString());
        entity.setBomNodeId(request.bomNodeId());
        entity.setToolingType(request.toolingType());
        entity.setDescription(request.description());
        entity.setCavityCount(request.cavityCount());
        entity.setToolCost(request.toolCost());
        entity.setToolLifeHits(request.toolLifeHits());
        entity.setRefurbishmentCostPerEvent(request.refurbishmentCostPerEvent());
        entity.setCumulativeVolumeToDate(request.cumulativeVolumeToDate());
        entity.setOwnership(request.ownership());
        entity.setSupplierId(request.supplierId());
        entity.setToolingQuoteReference(request.toolingQuoteReference());
        entity.setPlantLocation(request.plantLocation());
        entity.setInstallationDate(request.installationDate());
        entity.setAssetStatus(ToolingAssetStatus.ACTIVE);

        ToolingRecord saved = repository.save(entity);
        auditService.recordCreate(ENTITY_TYPE, saved.getToolingRecordId(), toResponse(saved));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Response getById(String toolingRecordId) {
        return toResponse(repository.findByCustomerIdAndToolingRecordId(tenantAccessGuard.requireCustomerId(), toolingRecordId)
                .orElseThrow(() -> ResourceNotFoundException.of("ToolingRecord", toolingRecordId)));
    }

    /** TL-06: every tooling record for a part, for side-by-side supplier-proposal comparison. */
    @Transactional(readOnly = true)
    public List<Response> getByBomNode(String bomNodeId) {
        return repository.findByCustomerIdAndBomNodeId(tenantAccessGuard.requireCustomerId(), bomNodeId).stream().map(this::toResponse).toList();
    }

    /** TL-07: the asset register, optionally filtered by plant location. Retired tools remain listed. */
    @Transactional(readOnly = true)
    public List<Response> assetRegister(String plantLocation) {
        List<ToolingRecord> records = plantLocation == null || plantLocation.isBlank()
                ? repository.findByCustomerIdOrderByCreatedAtDesc(tenantAccessGuard.requireCustomerId())
                : repository.findByCustomerIdAndPlantLocation(tenantAccessGuard.requireCustomerId(), plantLocation);
        return records.stream().map(this::toResponse).toList();
    }

    /** TL-07: retiring a tool never deletes its asset-register history - status changes only. */
    @Transactional
    public Response retire(String toolingRecordId, RetireRequest request) {
        ToolingRecord record = repository.findByCustomerIdAndToolingRecordId(tenantAccessGuard.requireCustomerId(), toolingRecordId)
                .orElseThrow(() -> ResourceNotFoundException.of("ToolingRecord", toolingRecordId));
        Response before = toResponse(record);
        record.setAssetStatus(ToolingAssetStatus.RETIRED);
        ToolingRecord saved = repository.save(record);
        auditService.recordUpdate(ENTITY_TYPE, toolingRecordId, before, toResponse(saved));
        return toResponse(saved);
    }

    private Response toResponse(ToolingRecord r) {
        return new Response(r.getToolingRecordId(), r.getBomNodeId(), r.getToolingType(), r.getDescription(),
                r.getCavityCount(), r.getToolCost(), r.getToolLifeHits(), r.getRefurbishmentCostPerEvent(),
                r.getCumulativeVolumeToDate(), r.getOwnership(), r.getSupplierId(), r.getToolingQuoteReference(),
                r.getPlantLocation(), r.getInstallationDate(), r.getAssetStatus());
    }
}
