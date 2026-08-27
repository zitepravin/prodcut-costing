package com.automotive.costing.configuration.service;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.configuration.dto.ConfigurationDtos.CreatePackRequest;
import com.automotive.costing.configuration.dto.ConfigurationDtos.PackItemInput;
import com.automotive.costing.configuration.dto.ConfigurationDtos.PackResponse;
import com.automotive.costing.configuration.entity.QuestionnairePack;
import com.automotive.costing.configuration.entity.QuestionnairePackItem;
import com.automotive.costing.configuration.repository.QuestionnairePackItemRepository;
import com.automotive.costing.configuration.repository.QuestionnairePackRepository;
import com.automotive.costing.vendorselection.entity.VendorQuestion;
import com.automotive.costing.vendorselection.repository.VendorQuestionRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * P3-07b Questionnaire packs. §11: pack may not exclude active mandatory-gate questions.
 */
@Service
@RequiredArgsConstructor
public class QuestionnairePackService {

    private final QuestionnairePackRepository packRepository;
    private final QuestionnairePackItemRepository itemRepository;
    private final VendorQuestionRepository questionRepository;
    private final TenantAccessGuard tenantAccessGuard;

    @Transactional(readOnly = true)
    public List<PackResponse> list() {
        return packRepository.findAllByOrderByPackCodeAscVersionNoAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PackResponse create(CreatePackRequest request) {
        QuestionnairePack pack = new QuestionnairePack();
        pack.setPackId(UUID.randomUUID().toString());
        pack.setPackCode(request.packCode().trim().toUpperCase(Locale.ROOT));
        pack.setPackName(request.packName().trim());
        pack.setPurpose(request.purpose() == null || request.purpose().isBlank()
                ? "EVALUATION" : request.purpose().trim().toUpperCase(Locale.ROOT));
        pack.setStatus("DRAFT");
        pack.setVersionNo(1);
        pack.setDescription(request.description());
        packRepository.save(pack);
        replaceItems(pack.getPackId(), request.items());
        return toResponse(pack, List.of("Created as DRAFT. Publish after including all mandatory-gate questions."));
    }

    @Transactional
    public PackResponse publish(String packId) {
        QuestionnairePack pack = packRepository.findById(packId)
                .orElseThrow(() -> new ResourceNotFoundException("Questionnaire pack", packId));
        if ("ACTIVE".equals(pack.getStatus())) {
            return toResponse(pack, List.of("Already ACTIVE"));
        }
        if ("INACTIVE".equals(pack.getStatus())) {
            throw new BusinessRuleViolationException(
                    "Retired pack cannot be published; create a new version.", "CFG-QPACK-01");
        }
        assertMandatoryGatesIncluded(packId);
        pack.setStatus("ACTIVE");
        packRepository.save(pack);
        return toResponse(pack, List.of("Published ACTIVE. Historical evaluations should bind to this pack id."));
    }

    @Transactional
    public PackResponse retire(String packId) {
        QuestionnairePack pack = packRepository.findById(packId)
                .orElseThrow(() -> new ResourceNotFoundException("Questionnaire pack", packId));
        long active = packRepository.countByStatus("ACTIVE");
        if ("ACTIVE".equals(pack.getStatus()) && active <= 1) {
            // Soft policy: allow retire last questionnaire pack (unlike scorecard weights) —
            // evaluation can still use full question bank. Document in messages.
        }
        pack.setStatus("INACTIVE");
        packRepository.save(pack);
        return toResponse(pack, List.of("Retired INACTIVE"));
    }

    /**
     * §11 decision 4: No — pack may not exclude mandatory-gate questions that are active.
     */
    private void assertMandatoryGatesIncluded(String packId) {
        List<VendorQuestion> mandatory = questionRepository
                .findByCustomerIdAndActiveFlagTrueOrderByCategoryAscQuestionCodeAsc(tenantAccessGuard.requireCustomerId())
                .stream()
                .filter(VendorQuestion::isMandatoryGate)
                .toList();
        if (mandatory.isEmpty()) {
            return;
        }
        Set<String> inPack = itemRepository.findByPackIdOrderBySortOrderAsc(packId).stream()
                .map(QuestionnairePackItem::getQuestionId)
                .collect(Collectors.toSet());
        List<String> missing = new ArrayList<>();
        for (VendorQuestion q : mandatory) {
            if (!inPack.contains(q.getQuestionId())) {
                missing.add(q.getQuestionCode());
            }
        }
        if (!missing.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "Pack must include all active mandatory-gate questions (missing: "
                            + String.join(", ", missing) + ")",
                    "CFG-QPACK-GATE");
        }
    }

    private void replaceItems(String packId, List<PackItemInput> items) {
        itemRepository.deleteByPackId(packId);
        if (items == null) {
            return;
        }
        Set<String> seen = new HashSet<>();
        int order = 0;
        for (PackItemInput in : items) {
            if (in.questionId() == null || in.questionId().isBlank() || !seen.add(in.questionId())) {
                continue;
            }
            questionRepository.findById(in.questionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Vendor question", in.questionId()));
            QuestionnairePackItem item = new QuestionnairePackItem();
            item.setItemId(UUID.randomUUID().toString());
            item.setPackId(packId);
            item.setQuestionId(in.questionId());
            item.setSortOrder(in.sortOrder() > 0 ? in.sortOrder() : order++);
            itemRepository.save(item);
        }
    }

    private PackResponse toResponse(QuestionnairePack pack) {
        return toResponse(pack, List.of());
    }

    private PackResponse toResponse(QuestionnairePack pack, List<String> messages) {
        List<PackItemInput> items = itemRepository.findByPackIdOrderBySortOrderAsc(pack.getPackId()).stream()
                .map(i -> new PackItemInput(i.getQuestionId(), i.getSortOrder()))
                .toList();
        return new PackResponse(
                pack.getPackId(), pack.getPackCode(), pack.getPackName(), pack.getPurpose(),
                pack.getStatus(), pack.getVersionNo(), pack.getDescription(), items, messages);
    }
}
