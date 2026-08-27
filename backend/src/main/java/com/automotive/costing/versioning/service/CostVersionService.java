package com.automotive.costing.versioning.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.versioning.dto.VersionControlDtos.CompareResponse;
import com.automotive.costing.versioning.dto.VersionControlDtos.CreateSnapshotRequest;
import com.automotive.costing.versioning.dto.VersionControlDtos.TimelineEntry;
import com.automotive.costing.versioning.dto.VersionControlDtos.VersionResponse;
import com.automotive.costing.versioning.entity.ChangeTrigger;
import com.automotive.costing.versioning.entity.CostVersionRecord;
import com.automotive.costing.versioning.repository.CostVersionRecordRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * M15 Version Control — immutable snapshots, baseline designation, rollback, timeline, compare.
 *
 * <p><b>Automated versioning (VER-01)</b> — domain services call
 * {@link #snapshotAfterChange} on significant state transitions:
 * <ul>
 *   <li>{@code ShouldCostService} — should-cost generate / significant recalculation ({@link ChangeTrigger#MANUAL_REVISION})</li>
 *   <li>{@code SupplierQuoteService} — quote ACCEPTED ({@link ChangeTrigger#RFQ_ROUND})</li>
 *   <li>{@code TargetCostingService} — target activated ({@link ChangeTrigger#APPROVAL_BASELINE})</li>
 *   <li>{@code BomNodeService} — BOM node release with ECN ({@link ChangeTrigger#ECN})</li>
 * </ul>
 * Manual {@link #createSnapshot} remains available for engineer-driven revisions.
 * Snapshots are append-only; rollback creates a <em>new</em> version (history preserved).
 */
@Service
@RequiredArgsConstructor
public class CostVersionService {

    public static final String TYPE_SHOULD_COST = "ShouldCostBuildUp";
    public static final String TYPE_SUPPLIER_QUOTE = "SupplierQuote";
    public static final String TYPE_TARGET_COST = "TargetCostRecord";
    public static final String TYPE_BOM_NODE = "BomNode";

    private final CostVersionRecordRepository repository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Transactional
    public VersionResponse createSnapshot(CreateSnapshotRequest request) {
        if (request.changeTrigger() == null) {
            throw new BusinessRuleViolationException(
                    "Change trigger is required before saving a new version (VER-02).", "VER-02");
        }
        int next = repository.findFirstByEntityTypeAndEntityIdOrderByVersionNumberDesc(
                        request.entityType(), request.entityId())
                .map(v -> v.getVersionNumber() + 1)
                .orElse(1);

        Map<String, Object> snapshot = request.snapshot() != null && !request.snapshot().isEmpty()
                ? request.snapshot()
                : minimalSnapshot(request.entityType(), request.entityId(), next);

        CostVersionRecord entity = new CostVersionRecord();
        entity.setVersionId(UUID.randomUUID().toString());
        entity.setEntityType(request.entityType());
        entity.setEntityId(request.entityId());
        entity.setVersionNumber(next);
        entity.setChangeTrigger(request.changeTrigger());
        entity.setChangeReference(request.changeReference());
        entity.setChangeComment(request.changeComment());
        entity.setSnapshotJson(writeJson(snapshot));
        entity.setProgramBaseline(false);

        CostVersionRecord saved = repository.save(entity);
        auditService.recordCreate("CostVersionRecord", saved.getVersionId(), toResponse(saved));
        return toResponse(saved);
    }

    /**
     * Convenience for domain services: snapshot after an approved record is modified (VER-01).
     */
    @Transactional
    public VersionResponse snapshotAfterChange(
            String entityType,
            String entityId,
            ChangeTrigger trigger,
            String reference,
            String comment,
            Map<String, Object> snapshot) {
        return createSnapshot(new CreateSnapshotRequest(entityType, entityId, trigger, reference, comment, snapshot));
    }

    @Transactional(readOnly = true)
    public List<TimelineEntry> timeline(String entityType, String entityId) {
        return repository.findByEntityTypeAndEntityIdOrderByVersionNumberDesc(entityType, entityId).stream()
                .map(v -> new TimelineEntry(
                        v.getVersionId(),
                        v.getVersionNumber(),
                        v.getChangeTrigger(),
                        v.getChangeReference(),
                        v.getChangeComment(),
                        v.isProgramBaseline(),
                        v.getCreatedBy(),
                        v.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public VersionResponse getVersion(String versionId) {
        return toResponse(repository.findById(versionId)
                .orElseThrow(() -> ResourceNotFoundException.of("CostVersionRecord", versionId)));
    }

    @Transactional
    public VersionResponse designateBaseline(String versionId) {
        CostVersionRecord target = repository.findById(versionId)
                .orElseThrow(() -> ResourceNotFoundException.of("CostVersionRecord", versionId));
        repository.clearBaseline(target.getEntityType(), target.getEntityId());
        target.setProgramBaseline(true);
        CostVersionRecord saved = repository.save(target);
        auditService.recordUpdate("CostVersionRecord", saved.getVersionId(), null,
                "programBaseline=true version=" + saved.getVersionNumber());
        return toResponse(saved);
    }

    /**
     * VER-05: rollback creates a NEW version copying the prior snapshot (history preserved).
     */
    @Transactional
    public VersionResponse rollback(String versionId, String comment) {
        CostVersionRecord prior = repository.findById(versionId)
                .orElseThrow(() -> ResourceNotFoundException.of("CostVersionRecord", versionId));
        Map<String, Object> snap = readJson(prior.getSnapshotJson());
        snap.put("rolledBackFromVersion", prior.getVersionNumber());
        snap.put("rolledBackFromVersionId", prior.getVersionId());
        return createSnapshot(new CreateSnapshotRequest(
                prior.getEntityType(),
                prior.getEntityId(),
                ChangeTrigger.ROLLBACK,
                "v" + prior.getVersionNumber(),
                comment != null ? comment : "Rollback to version " + prior.getVersionNumber(),
                snap));
    }

    @Transactional(readOnly = true)
    public CompareResponse compare(String entityType, String entityId, int leftVersion, int rightVersion) {
        CostVersionRecord left = repository.findByEntityTypeAndEntityIdAndVersionNumber(entityType, entityId, leftVersion)
                .orElseThrow(() -> ResourceNotFoundException.of("CostVersionRecord",
                        entityType + "/" + entityId + "/v" + leftVersion));
        CostVersionRecord right = repository.findByEntityTypeAndEntityIdAndVersionNumber(entityType, entityId, rightVersion)
                .orElseThrow(() -> ResourceNotFoundException.of("CostVersionRecord",
                        entityType + "/" + entityId + "/v" + rightVersion));
        return new CompareResponse(entityType, entityId, toResponse(left), toResponse(right));
    }

    private Map<String, Object> minimalSnapshot(String entityType, String entityId, int versionNumber) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("entityType", entityType);
        m.put("entityId", entityId);
        m.put("versionNumber", versionNumber);
        m.put("note", "Server-side minimal snapshot; client may supply full payload.");
        return m;
    }

    private VersionResponse toResponse(CostVersionRecord v) {
        return new VersionResponse(
                v.getVersionId(),
                v.getEntityType(),
                v.getEntityId(),
                v.getVersionNumber(),
                v.getChangeTrigger(),
                v.getChangeReference(),
                v.getChangeComment(),
                v.isProgramBaseline(),
                v.getCreatedBy(),
                v.getCreatedAt(),
                readJson(v.getSnapshotJson()));
    }

    private String writeJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new BusinessRuleViolationException("Unable to serialize version snapshot.", "VER-01");
        }
    }

    private Map<String, Object> readJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            return Map.of("raw", json);
        }
    }
}
