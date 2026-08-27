package com.automotive.costing.erp.service;

import com.automotive.costing.bom.entity.BomNode;
import com.automotive.costing.bom.entity.BomNodeStatus;
import com.automotive.costing.bom.entity.MakeBuyFlag;
import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.erp.dto.ErpDtos.BatchResponse;
import com.automotive.costing.erp.dto.ErpDtos.PromoteResult;
import com.automotive.costing.erp.entity.ErpBatchStatus;
import com.automotive.costing.erp.entity.ErpBomStaging;
import com.automotive.costing.erp.entity.ErpInboundBatch;
import com.automotive.costing.erp.entity.ErpMaterialPriceStaging;
import com.automotive.costing.erp.entity.ErpPartStaging;
import com.automotive.costing.erp.entity.ErpStagingRowStatus;
import com.automotive.costing.erp.repository.ErpBomStagingRepository;
import com.automotive.costing.erp.repository.ErpInboundBatchRepository;
import com.automotive.costing.erp.repository.ErpMaterialPriceStagingRepository;
import com.automotive.costing.erp.repository.ErpPartStagingRepository;
import com.automotive.costing.masterdata.dto.MaterialMasterDtos.UpdateRequest;
import com.automotive.costing.masterdata.entity.VehicleProgram;
import com.automotive.costing.masterdata.repository.MaterialMasterRepository;
import com.automotive.costing.masterdata.repository.VehicleProgramRepository;
import com.automotive.costing.masterdata.service.MaterialMasterService;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * ERP promote-to-live: writes READY staging data into live masters.
 *
 * <ul>
 *   <li>MATERIAL_PRICE (VALID): new MaterialMaster version when code exists</li>
 *   <li>PART (VALID): acknowledged PROMOTED (CAD/PLM seam)</li>
 *   <li>BOM (VALID): creates/links BomNode under the program resolved by program_code</li>
 * </ul>
 *
 * Only READY batches may be promoted. Fully audited.
 */
@Service
@RequiredArgsConstructor
public class ErpPromoteService {

    private static final Logger log = LoggerFactory.getLogger(ErpPromoteService.class);
    private static final String ENTITY_TYPE = "ErpInboundBatch";
    private static final String BOM_ENTITY_TYPE = "BomNode";

    private final ErpInboundBatchRepository batchRepository;
    private final ErpPartStagingRepository partRepository;
    private final ErpBomStagingRepository bomRepository;
    private final ErpMaterialPriceStagingRepository priceRepository;
    private final MaterialMasterRepository materialMasterRepository;
    private final MaterialMasterService materialMasterService;
    private final TenantAccessGuard tenantAccessGuard;
    private final VehicleProgramRepository vehicleProgramRepository;
    private final BomNodeRepository bomNodeRepository;
    private final AuditService auditService;

    @Transactional
    public PromoteResult promote(String batchId) {
        ErpInboundBatch batch = batchRepository.findByCustomerIdAndBatchId(tenantAccessGuard.requireCustomerId(), batchId)
                .orElseThrow(() -> ResourceNotFoundException.of("ErpInboundBatch", batchId));

        if (batch.getStatus() != ErpBatchStatus.READY) {
            throw new BusinessRuleViolationException(
                    "Only READY batches can be promoted; current status is " + batch.getStatus(),
                    "ERP-03");
        }

        List<String> messages = new ArrayList<>();
        int pricesPromoted = 0;
        int pricesSkipped = 0;
        int partsAck = 0;
        int bomPromoted = 0;
        int bomSkipped = 0;

        Instant now = Instant.now();

        // --- Material prices → live MaterialMaster versions ---
        for (ErpMaterialPriceStaging row : priceRepository.findByBatchIdOrderByCreatedAtAsc(batchId)) {
            if (row.getStatus() != ErpStagingRowStatus.VALID) {
                if (row.getStatus() == ErpStagingRowStatus.INVALID) {
                    row.setStatus(ErpStagingRowStatus.SKIPPED);
                    row.setErrorMessage("Skipped: row was INVALID at validate");
                    row.setUpdatedAt(now);
                    priceRepository.save(row);
                    pricesSkipped++;
                }
                continue;
            }

            String code = row.getMaterialCode();
            if (!materialMasterRepository.existsByCustomerIdAndCode(tenantAccessGuard.requireCustomerId(), code)) {
                row.setStatus(ErpStagingRowStatus.SKIPPED);
                row.setErrorMessage("Skipped: material code '" + code + "' not found in live master");
                row.setUpdatedAt(now);
                priceRepository.save(row);
                pricesSkipped++;
                messages.add("price " + row.getStagingId() + ": material " + code + " not in master");
                continue;
            }

            try {
                UpdateRequest update = new UpdateRequest(
                        null,
                        row.getUnitPrice(),
                        row.getUom(),
                        row.getEffectiveFrom(),
                        row.getEffectiveTo(),
                        null, null, null, null, null, null, null, null, null
                );
                materialMasterService.createNewVersion(code, update);
                row.setStatus(ErpStagingRowStatus.PROMOTED);
                row.setErrorMessage(null);
                row.setUpdatedAt(now);
                priceRepository.save(row);
                pricesPromoted++;
            } catch (Exception ex) {
                log.warn("Promote material price failed for staging {}: {}", row.getStagingId(), ex.getMessage());
                row.setStatus(ErpStagingRowStatus.SKIPPED);
                row.setErrorMessage("Promote failed: " + ex.getMessage());
                row.setUpdatedAt(now);
                priceRepository.save(row);
                pricesSkipped++;
                messages.add("price " + row.getStagingId() + ": " + ex.getMessage());
            }
        }

        // --- Parts: acknowledge (PROMOTED) — enrich used by BOM promote ---
        Map<String, ErpPartStaging> partsByNumber = new HashMap<>();
        for (ErpPartStaging row : partRepository.findByBatchIdOrderByCreatedAtAsc(batchId)) {
            if (row.getStatus() != ErpStagingRowStatus.VALID) {
                continue;
            }
            row.setStatus(ErpStagingRowStatus.PROMOTED);
            row.setErrorMessage(null);
            row.setUpdatedAt(now);
            partRepository.save(row);
            partsAck++;
            if (row.getPartNumber() != null) {
                partsByNumber.put(row.getPartNumber().trim().toUpperCase(), row);
            }
        }

        // --- BOM lines: program-scoped tree promote ---
        List<ErpBomStaging> bomRows = bomRepository.findByBatchIdOrderByLineNumberAsc(batchId).stream()
                .filter(r -> r.getStatus() == ErpStagingRowStatus.VALID)
                .sorted(Comparator.comparing(r -> r.getLineNumber() != null ? r.getLineNumber() : Integer.MAX_VALUE))
                .toList();

        // Cache program resolution and in-memory part→node maps per program
        Map<String, Optional<VehicleProgram>> programCache = new HashMap<>();
        Map<String, Map<String, BomNode>> nodesByProgramAndPart = new HashMap<>();

        for (ErpBomStaging row : bomRows) {
            String programCode = row.getProgramCode();
            if (programCode == null || programCode.isBlank()) {
                skipBom(row, now, "Missing program_code on BOM staging row");
                bomSkipped++;
                messages.add("bom " + row.getStagingId() + ": missing program_code");
                continue;
            }

            String codeKey = programCode.trim().toUpperCase();
            Optional<VehicleProgram> programOpt = programCache.computeIfAbsent(codeKey,
                    k -> vehicleProgramRepository.findByProgramCode(programCode.trim()));

            if (programOpt.isEmpty()) {
                skipBom(row, now, "Program code '" + programCode + "' not found");
                bomSkipped++;
                messages.add("bom " + row.getStagingId() + ": program " + programCode + " not found");
                continue;
            }

            VehicleProgram program = programOpt.get();
            String programId = program.getProgramId();

            Map<String, BomNode> partIndex = nodesByProgramAndPart.computeIfAbsent(programId, pid -> {
                Map<String, BomNode> idx = new HashMap<>();
                for (BomNode n : bomNodeRepository.findFullTreeByProgram(pid)) {
                    if (n.isWhatIfBranchFlag()) {
                        continue;
                    }
                    String key = n.getPartNumber() != null ? n.getPartNumber().trim().toUpperCase() : null;
                    if (key != null) {
                        idx.putIfAbsent(key, n);
                    }
                }
                return idx;
            });

            try {
                String parentKey = row.getParentPartNumber().trim().toUpperCase();
                String childKey = row.getChildPartNumber().trim().toUpperCase();

                BomNode parent = partIndex.get(parentKey);
                if (parent == null) {
                    parent = createBomNode(
                            programId,
                            null,
                            row.getParentPartNumber(),
                            row.getParentRevision(),
                            BigDecimal.ONE,
                            row.getUom() != null ? row.getUom() : "EA",
                            partsByNumber.get(parentKey),
                            now
                    );
                    partIndex.put(parentKey, parent);
                }

                BomNode existingChild = partIndex.get(childKey);
                if (existingChild != null && parent.getBomNodeId().equals(existingChild.getParentNodeId())) {
                    // Already linked under this parent — treat as promoted (idempotent)
                    row.setStatus(ErpStagingRowStatus.PROMOTED);
                    row.setErrorMessage(null);
                    row.setUpdatedAt(now);
                    bomRepository.save(row);
                    bomPromoted++;
                    continue;
                }

                if (existingChild != null && existingChild.getParentNodeId() != null
                        && !existingChild.getParentNodeId().equals(parent.getBomNodeId())) {
                    // Same part under different parent — create additional occurrence
                    existingChild = null;
                }

                if (existingChild == null) {
                    BomNode child = createBomNode(
                            programId,
                            parent.getBomNodeId(),
                            row.getChildPartNumber(),
                            row.getChildRevision(),
                            row.getQuantityPer(),
                            row.getUom() != null ? row.getUom() : "EA",
                            partsByNumber.get(childKey),
                            now
                    );
                    partIndex.put(childKey, child);
                }

                row.setStatus(ErpStagingRowStatus.PROMOTED);
                row.setErrorMessage(null);
                row.setUpdatedAt(now);
                bomRepository.save(row);
                bomPromoted++;
            } catch (Exception ex) {
                log.warn("BOM promote failed for staging {}: {}", row.getStagingId(), ex.getMessage());
                skipBom(row, now, "Promote failed: " + ex.getMessage());
                bomSkipped++;
                messages.add("bom " + row.getStagingId() + ": " + ex.getMessage());
            }
        }

        String statusBefore = batch.getStatus().name();
        batch.setStatus(ErpBatchStatus.PROMOTED);
        batch.setPromotedAt(now);
        batch.setPromotedBy(currentUserId());
        batch.setUpdatedAt(now);
        batchRepository.save(batch);

        BatchResponse after = toBatchResponse(batch);
        auditService.recordUpdate(ENTITY_TYPE, batch.getBatchId(), statusBefore, after);

        log.info("Promoted ERP batch {} — prices={}/{}, parts={}, bom={}/{}",
                batchId, pricesPromoted, pricesSkipped, partsAck, bomPromoted, bomSkipped);

        return new PromoteResult(
                batchId,
                batch.getStatus(),
                pricesPromoted,
                pricesSkipped,
                partsAck,
                bomPromoted,
                bomSkipped,
                messages
        );
    }

    private BomNode createBomNode(
            String programId,
            String parentNodeId,
            String partNumber,
            String revision,
            BigDecimal quantityPer,
            String uom,
            ErpPartStaging partInfo,
            Instant now
    ) {
        BomNode node = new BomNode();
        node.setCustomerId(tenantAccessGuard.requireCustomerId());
        node.setBomNodeId(UUID.randomUUID().toString());
        node.setParentNodeId(parentNodeId);
        node.setProgramId(programId);
        node.setPartNumber(partNumber);
        node.setRevision(revision);
        node.setQuantityPer(quantityPer != null ? quantityPer : BigDecimal.ONE);
        node.setUom(uom != null ? uom : "EA");
        node.setStatus(BomNodeStatus.DRAFT);
        node.setWhatIfBranchFlag(false);
        node.setOverrideFlag(false);

        if (partInfo != null) {
            if (partInfo.getNetWeightKg() != null) {
                node.setNetWeight(partInfo.getNetWeightKg());
            }
            if (partInfo.getMaterialCode() != null && !partInfo.getMaterialCode().isBlank()) {
                node.setMaterialCode(partInfo.getMaterialCode());
            }
            if (partInfo.getMakeBuyCode() != null) {
                String mb = partInfo.getMakeBuyCode().trim().toUpperCase();
                if ("BUY".equals(mb)) {
                    node.setMakeBuyFlag(MakeBuyFlag.BUY);
                } else if ("MAKE".equals(mb)) {
                    node.setMakeBuyFlag(MakeBuyFlag.MAKE);
                }
            }
            if (partInfo.getUom() != null && !partInfo.getUom().isBlank() && (uom == null || "EA".equals(uom))) {
                node.setUom(partInfo.getUom());
            }
        }

        BomNode saved = bomNodeRepository.save(node);
        auditService.recordCreate(BOM_ENTITY_TYPE, saved.getBomNodeId(),
                Map.of(
                        "partNumber", saved.getPartNumber(),
                        "programId", saved.getProgramId(),
                        "parentNodeId", saved.getParentNodeId() != null ? saved.getParentNodeId() : "",
                        "source", "ERP_PROMOTE"
                ));
        return saved;
    }

    private void skipBom(ErpBomStaging row, Instant now, String reason) {
        row.setStatus(ErpStagingRowStatus.SKIPPED);
        row.setErrorMessage(reason);
        row.setUpdatedAt(now);
        bomRepository.save(row);
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
                b.getProcessedAt()
        );
    }

    private String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "system";
        }
        return auth.getName();
    }
}
