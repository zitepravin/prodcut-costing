package com.automotive.costing.vendorselection.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.rfq.service.SupplierMasterService;
import com.automotive.costing.vendorselection.service.SupplierLifecycleGuard;
import com.automotive.costing.vendorselection.engine.ScorecardScoringEngine;
import com.automotive.costing.vendorselection.engine.ScorecardScoringEngine.CategoryScoreInput;
import com.automotive.costing.vendorselection.engine.ScorecardScoringEngine.CategoryWeight;
import com.automotive.costing.vendorselection.entity.ScorecardTemplateStatus;
import com.automotive.costing.vendorselection.entity.VendorScorecardCategory;
import com.automotive.costing.vendorselection.entity.VendorScorecardResult;
import com.automotive.costing.vendorselection.entity.VendorScorecardTemplate;
import com.automotive.costing.vendorselection.repository.VendorScorecardCategoryRepository;
import com.automotive.costing.vendorselection.repository.VendorScorecardResultRepository;
import com.automotive.costing.vendorselection.repository.VendorScorecardTemplateRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScorecardService {

    private final VendorScorecardTemplateRepository templateRepository;
    private final TenantAccessGuard tenantAccessGuard;
    private final VendorScorecardCategoryRepository categoryRepository;
    private final VendorScorecardResultRepository resultRepository;
    private final ScorecardScoringEngine scoringEngine;
    private final SupplierMasterService supplierMasterService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final SupplierLifecycleGuard lifecycleGuard;

    public record CategoryDef(String categoryCode, String categoryName, BigDecimal weightPercent, int sequenceNo) {}

    public record CreateTemplateRequest(
            String templateCode,
            String templateName,
            String description,
            List<CategoryDef> categories
    ) {}

    public record TemplateResponse(
            String templateId,
            String templateCode,
            String templateName,
            String description,
            String status,
            List<CategoryDef> categories,
            String parentTemplateId,
            String lineageRootId,
            int versionNo,
            String versionLabel,
            boolean systemDefault
    ) {}

    public record CategoryScoreRequest(String categoryCode, BigDecimal rawScore0to100) {}

    public record EvaluateScorecardRequest(
            String templateId,
            String supplierId,
            String bomNodeId,
            List<CategoryScoreRequest> categoryScores,
            String notes
    ) {}

    public record ScorecardResultResponse(
            String resultId,
            String templateId,
            String supplierId,
            String bomNodeId,
            BigDecimal overallScore,
            BigDecimal maxScore,
            List<ScorecardScoringEngine.CategoryScoreLine> categoryScores,
            String notes,
            Instant scoredAt
    ) {}

    @Transactional
    public TemplateResponse createTemplate(CreateTemplateRequest request) {
        if (request.templateCode() == null || request.templateCode().isBlank()) {
            throw new BusinessRuleViolationException("templateCode is required", "VS-SC-07");
        }
        String customerId = tenantAccessGuard.requireCustomerId();
        if (templateRepository.existsByCustomerIdAndTemplateCodeIgnoreCase(customerId, request.templateCode())) {
            throw new BusinessRuleViolationException("Template code already exists", "VS-SC-08");
        }
        List<CategoryWeight> weights = new ArrayList<>();
        if (request.categories() != null) {
            for (CategoryDef c : request.categories()) {
                weights.add(new CategoryWeight(c.categoryCode(), c.categoryName(), c.weightPercent()));
            }
        }
        scoringEngine.assertWeightsTotal100(weights);

        VendorScorecardTemplate t = new VendorScorecardTemplate();
        t.setCustomerId(tenantAccessGuard.requireCustomerId());
        t.setTemplateId(UUID.randomUUID().toString());
        t.setTemplateCode(request.templateCode().trim().toUpperCase());
        t.setTemplateName(request.templateName().trim());
        t.setDescription(request.description());
        t.setStatus(ScorecardTemplateStatus.ACTIVE);
        t.setParentTemplateId(null);
        t.setVersionNo(1);
        t.setVersionLabel("v1");
        templateRepository.save(t);
        // Root lineage is self when created from scratch
        t.setLineageRootId(t.getTemplateId());
        templateRepository.save(t);

        List<CategoryDef> savedCats = new ArrayList<>();
        int seq = 0;
        for (CategoryDef c : request.categories()) {
            VendorScorecardCategory cat = new VendorScorecardCategory();
            cat.setCategoryId(UUID.randomUUID().toString());
            cat.setTemplateId(t.getTemplateId());
            cat.setCategoryCode(c.categoryCode().trim().toUpperCase());
            cat.setCategoryName(c.categoryName().trim());
            cat.setWeightPercent(c.weightPercent());
            cat.setSequenceNo(c.sequenceNo() > 0 ? c.sequenceNo() : seq++);
            categoryRepository.save(cat);
            savedCats.add(new CategoryDef(cat.getCategoryCode(), cat.getCategoryName(),
                    cat.getWeightPercent(), cat.getSequenceNo()));
        }
        auditService.recordCreate("VendorScorecardTemplate", t.getTemplateId(), t.getTemplateCode());
        return toTemplate(t);
    }

    @Transactional(readOnly = true)
    public List<TemplateResponse> listTemplates() {
        return templateRepository.findByCustomerIdOrderByTemplateCodeAsc(tenantAccessGuard.requireCustomerId())
                .stream().map(this::toTemplate).toList();
    }

    @Transactional(readOnly = true)
    public TemplateResponse getTemplate(String templateId) {
        return toTemplate(requireTemplate(templateId));
    }

    @Transactional(readOnly = true)
    public List<TemplateResponse> listLineage(String templateId) {
        VendorScorecardTemplate t = requireTemplate(templateId);
        String root = t.getLineageRootId() != null ? t.getLineageRootId() : t.getTemplateId();
        return templateRepository.findByCustomerIdAndLineageRootIdOrderByVersionNoAsc(tenantAccessGuard.requireCustomerId(), root).stream()
                .map(this::toTemplate)
                .toList();
    }

    public record CloneTemplateRequest(
            String newTemplateCode,
            String newTemplateName,
            String description,
            /** When null/empty, categories are copied from source. When provided, replaces categories (must total 100%). */
            List<CategoryDef> categories
    ) {}

    /**
     * Customer-editable weights: clone an existing template (e.g. VS-DEFAULT-100) with optional new weights.
     * Source template is never mutated. Historical evaluations remain bound to the original template id.
     */
    @Transactional
    public TemplateResponse cloneTemplate(String sourceTemplateId, CloneTemplateRequest request) {
        VendorScorecardTemplate source = requireTemplate(sourceTemplateId);
        if (request == null || request.newTemplateCode() == null || request.newTemplateCode().isBlank()) {
            throw new BusinessRuleViolationException("newTemplateCode is required", "VS-SC-10");
        }
        if (request.newTemplateName() == null || request.newTemplateName().isBlank()) {
            throw new BusinessRuleViolationException("newTemplateName is required", "VS-SC-11");
        }
        String code = request.newTemplateCode().trim().toUpperCase();
        if (templateRepository.existsByTemplateCodeIgnoreCase(code)) {
            throw new BusinessRuleViolationException("Template code already exists", "VS-SC-08");
        }

        List<CategoryDef> categories;
        if (request.categories() != null && !request.categories().isEmpty()) {
            categories = request.categories();
        } else {
            categories = categoryRepository.findByTemplateIdOrderBySequenceNoAsc(source.getTemplateId())
                    .stream()
                    .map(c -> new CategoryDef(c.getCategoryCode(), c.getCategoryName(),
                            c.getWeightPercent(), c.getSequenceNo()))
                    .toList();
        }

        List<CategoryWeight> weights = categories.stream()
                .map(c -> new CategoryWeight(c.categoryCode(), c.categoryName(), c.weightPercent()))
                .toList();
        scoringEngine.assertWeightsTotal100(weights);

        String rootId = source.getLineageRootId() != null ? source.getLineageRootId() : source.getTemplateId();
        int nextVersion = templateRepository.countByLineageRootId(rootId) + 1;

        VendorScorecardTemplate t = new VendorScorecardTemplate();
        t.setCustomerId(tenantAccessGuard.requireCustomerId());
        t.setTemplateId(UUID.randomUUID().toString());
        t.setTemplateCode(code);
        t.setTemplateName(request.newTemplateName().trim());
        t.setDescription(request.description() != null ? request.description() : source.getDescription());
        t.setStatus(ScorecardTemplateStatus.ACTIVE);
        t.setParentTemplateId(source.getTemplateId());
        t.setLineageRootId(rootId);
        t.setVersionNo(nextVersion);
        t.setVersionLabel(request.newTemplateCode() != null
                ? "v" + nextVersion + "-from-" + source.getTemplateCode()
                : "v" + nextVersion);
        templateRepository.save(t);

        for (CategoryDef c : categories) {
            VendorScorecardCategory cat = new VendorScorecardCategory();
            cat.setCategoryId(UUID.randomUUID().toString());
            cat.setTemplateId(t.getTemplateId());
            cat.setCategoryCode(c.categoryCode().trim().toUpperCase());
            cat.setCategoryName(c.categoryName().trim());
            cat.setWeightPercent(c.weightPercent());
            cat.setSequenceNo(c.sequenceNo());
            categoryRepository.save(cat);
        }
        auditService.recordCreate("VendorScorecardTemplate", t.getTemplateId(),
                t.getTemplateCode() + " parent=" + source.getTemplateId() + " v" + nextVersion);
        return toTemplate(t);
    }

    /**
     * Deactivate a template so it is not used for new evaluations.
     * Templates that already have results remain immutable for history (categories not rewritten).
     */
    /**
     * P3-07a §11 decision 3: forbid retiring the last ACTIVE (published) template.
     * System default may be retired only if another ACTIVE template exists.
     */
    @Transactional
    public TemplateResponse deactivateTemplate(String templateId) {
        VendorScorecardTemplate t = requireTemplate(templateId);
        if (t.getStatus() == ScorecardTemplateStatus.INACTIVE) {
            return toTemplate(t);
        }
        long activeCount = templateRepository.countByCustomerIdAndStatus(tenantAccessGuard.requireCustomerId(), ScorecardTemplateStatus.ACTIVE);
        if (t.getStatus() == ScorecardTemplateStatus.ACTIVE && activeCount <= 1) {
            throw new BusinessRuleViolationException(
                    "Cannot retire the last PUBLISHED/ACTIVE scorecard weight set (P3-07a policy).",
                    "VS-SC-07A-LAST");
        }
        String from = t.getStatus().name();
        t.setStatus(ScorecardTemplateStatus.INACTIVE);
        templateRepository.save(t);
        auditService.recordUpdate("VendorScorecardTemplate", templateId, from, "INACTIVE");
        return toTemplate(t);
    }

    /**
     * P3-07a: DRAFT → ACTIVE (published). Weights must total 100%.
     */
    @Transactional
    public TemplateResponse publishTemplate(String templateId) {
        VendorScorecardTemplate t = requireTemplate(templateId);
        if (t.getStatus() == ScorecardTemplateStatus.ACTIVE) {
            return toTemplate(t);
        }
        if (t.getStatus() == ScorecardTemplateStatus.INACTIVE) {
            throw new BusinessRuleViolationException(
                    "Retired template cannot be published; clone a new version instead.",
                    "VS-SC-07A-RETIRED");
        }
        List<VendorScorecardCategory> cats =
                categoryRepository.findByTemplateIdOrderBySequenceNoAsc(templateId);
        List<CategoryWeight> weights = cats.stream()
                .map(c -> new CategoryWeight(c.getCategoryCode(), c.getCategoryName(), c.getWeightPercent()))
                .toList();
        scoringEngine.assertWeightsTotal100(weights);
        t.setStatus(ScorecardTemplateStatus.ACTIVE);
        templateRepository.save(t);
        auditService.recordUpdate("VendorScorecardTemplate", templateId, "DRAFT", "ACTIVE");
        return toTemplate(t);
    }

    /**
     * P3-07a §11 decision 2: resolve system default ACTIVE template when id omitted.
     */
    @Transactional(readOnly = true)
    public TemplateResponse resolveEffectiveTemplate(String templateIdOrNull) {
        if (templateIdOrNull != null && !templateIdOrNull.isBlank()) {
            return getTemplate(templateIdOrNull);
        }
        VendorScorecardTemplate def = templateRepository
                .findFirstBySystemDefaultTrueAndStatus(ScorecardTemplateStatus.ACTIVE)
                .orElseGet(() -> templateRepository.findByStatusOrderByTemplateCodeAsc(ScorecardTemplateStatus.ACTIVE)
                        .stream().findFirst()
                        .orElseThrow(() -> new BusinessRuleViolationException(
                                "No ACTIVE scorecard template available (system default missing).",
                                "VS-SC-07A-NODEFAULT")));
        return toTemplate(def);
    }

    /**
     * Replace categories/weights only when the template has never been used in an evaluation.
     * Once used, clone a new template instead (immutability of historical scorecards).
     */
    @Transactional
    public TemplateResponse replaceCategoriesIfUnused(String templateId, List<CategoryDef> categories) {
        VendorScorecardTemplate t = requireTemplate(templateId);
        long used = resultRepository.countByTemplateId(templateId);
        if (used > 0) {
            throw new BusinessRuleViolationException(
                    "Template already has evaluation results and cannot change weights. Clone a new template instead.",
                    "VS-SC-12");
        }
        if (categories == null || categories.isEmpty()) {
            throw new BusinessRuleViolationException("categories are required", "VS-SC-13");
        }
        List<CategoryWeight> weights = categories.stream()
                .map(c -> new CategoryWeight(c.categoryCode(), c.categoryName(), c.weightPercent()))
                .toList();
        scoringEngine.assertWeightsTotal100(weights);

        for (VendorScorecardCategory existing : categoryRepository.findByTemplateIdOrderBySequenceNoAsc(templateId)) {
            categoryRepository.delete(existing);
        }
        int seq = 0;
        for (CategoryDef c : categories) {
            VendorScorecardCategory cat = new VendorScorecardCategory();
            cat.setCategoryId(UUID.randomUUID().toString());
            cat.setTemplateId(templateId);
            cat.setCategoryCode(c.categoryCode().trim().toUpperCase());
            cat.setCategoryName(c.categoryName().trim());
            cat.setWeightPercent(c.weightPercent());
            cat.setSequenceNo(c.sequenceNo() > 0 ? c.sequenceNo() : seq++);
            categoryRepository.save(cat);
        }
        auditService.recordUpdate("VendorScorecardTemplate", templateId, null, "categories-replaced");
        return toTemplate(t);
    }


    @Transactional(readOnly = true)
    public List<ScorecardResultResponse> listResultsBySupplier(String supplierId) {
        supplierMasterService.require(supplierId);
        return resultRepository.findByCustomerIdAndSupplierIdOrderByScoredAtDesc(tenantAccessGuard.requireCustomerId(), supplierId).stream()
                .map(r -> new ScorecardResultResponse(
                        r.getResultId(), r.getTemplateId(), r.getSupplierId(), r.getBomNodeId(),
                        r.getOverallScore(), r.getMaxScore(),
                        List.of(), // category lines stored in result detail if present; empty when historical snapshot only
                        r.getNotes(), r.getScoredAt()))
                .toList();
    }

    @Transactional
    public ScorecardResultResponse evaluate(EvaluateScorecardRequest request) {
        VendorScorecardTemplate template;
        String customerId = tenantAccessGuard.requireCustomerId();
        if (request.templateId() == null || request.templateId().isBlank()) {
            template = templateRepository
                    .findFirstByCustomerIdAndSystemDefaultTrueAndStatus(customerId, ScorecardTemplateStatus.ACTIVE)
                    .orElseGet(() -> templateRepository.findByCustomerIdAndStatusOrderByTemplateCodeAsc(customerId, ScorecardTemplateStatus.ACTIVE)
                            .stream().findFirst()
                            .orElseThrow(() -> new BusinessRuleViolationException(
                                    "No ACTIVE scorecard template (system default required).",
                                    "VS-SC-07A-NODEFAULT")));
        } else {
            template = requireTemplate(request.templateId());
        }
        if (template.getStatus() != ScorecardTemplateStatus.ACTIVE) {
            throw new BusinessRuleViolationException("Scorecard template is not ACTIVE/PUBLISHED", "VS-SC-09");
        }
        supplierMasterService.require(request.supplierId());
        lifecycleGuard.assertNotBlockedOrSuspended(
                request.supplierId(), SupplierLifecycleGuard.OP_SCORECARD_EVALUATE);

        List<VendorScorecardCategory> cats =
                categoryRepository.findByTemplateIdOrderBySequenceNoAsc(template.getTemplateId());
        List<CategoryWeight> weights = cats.stream()
                .map(c -> new CategoryWeight(c.getCategoryCode(), c.getCategoryName(), c.getWeightPercent()))
                .toList();
        List<CategoryScoreInput> inputs = request.categoryScores() == null ? List.of()
                : request.categoryScores().stream()
                .map(s -> new CategoryScoreInput(
                        s.categoryCode() == null ? null : s.categoryCode().trim().toUpperCase(),
                        s.rawScore0to100()))
                .toList();

        var output = scoringEngine.score(weights, inputs);

        VendorScorecardResult result = new VendorScorecardResult();
        result.setCustomerId(tenantAccessGuard.requireCustomerId());
        result.setResultId(UUID.randomUUID().toString());
        result.setTemplateId(template.getTemplateId());
        result.setSupplierId(request.supplierId());
        result.setBomNodeId(request.bomNodeId());
        result.setOverallScore(output.overallScore());
        result.setMaxScore(output.maxScore());
        try {
            result.setCategoryScoresJson(objectMapper.writeValueAsString(output.lines()));
        } catch (JsonProcessingException e) {
            result.setCategoryScoresJson("[]");
        }
        result.setNotes(request.notes());
        result.setScoredAt(Instant.now());
        result.setScoredBy(currentUserId());
        resultRepository.save(result);
        auditService.recordCreate("VendorScorecardResult", result.getResultId(),
                request.supplierId() + " score=" + output.overallScore());

        return new ScorecardResultResponse(
                result.getResultId(), result.getTemplateId(), result.getSupplierId(),
                result.getBomNodeId(), output.overallScore(), output.maxScore(),
                output.lines(), result.getNotes(), result.getScoredAt());
    }

    private VendorScorecardTemplate requireTemplate(String templateId) {
        String customerId = tenantAccessGuard.requireCustomerId();
        return templateRepository.findByCustomerIdAndTemplateId(customerId, templateId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Scorecard template not found or not accessible in current tenant: " + templateId));
    }

    private TemplateResponse toTemplate(VendorScorecardTemplate t) {
        List<CategoryDef> cats = categoryRepository.findByTemplateIdOrderBySequenceNoAsc(t.getTemplateId())
                .stream()
                .map(c -> new CategoryDef(c.getCategoryCode(), c.getCategoryName(),
                        c.getWeightPercent(), c.getSequenceNo()))
                .toList();
        return new TemplateResponse(t.getTemplateId(), t.getTemplateCode(), t.getTemplateName(),
                t.getDescription(), t.getStatus().name(), cats,
                t.getParentTemplateId(), t.getLineageRootId(),
                t.getVersionNo(), t.getVersionLabel(),
                t.isSystemDefault());
    }

    private String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof com.automotive.costing.platform.user.entity.User u) {
            return u.getUserId();
        }
        return null;
    }
}
