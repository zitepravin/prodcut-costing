package com.automotive.costing.platform.access.service;

import com.automotive.costing.masterdata.entity.MasterDataStatus;
import com.automotive.costing.masterdata.entity.OverheadPool;
import com.automotive.costing.masterdata.repository.OverheadPoolRepository;
import com.automotive.costing.vendorselection.entity.VendorQuestion;
import com.automotive.costing.vendorselection.repository.VendorQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Copies DEFAULT-tenant catalog templates into a newly created customer so
 * overhead OHD-02 minimum pools and questionnaire banks are available without
 * manual re-entry. Does not copy transactional data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantCatalogSeedService {

    private final OverheadPoolRepository overheadPoolRepository;
    private final VendorQuestionRepository vendorQuestionRepository;

    @Value("${app.tenant.seed-from-default-customer-code:DEFAULT}")
    private String defaultCustomerCode;

    @Value("${app.tenant.seed-catalog-on-create:true}")
    private boolean seedEnabled;

    /**
     * @param sourceCustomerId DEFAULT (or configured) customer id holding templates
     * @param targetCustomerId newly created customer
     */
    @Transactional
    public void seedFromDefault(String sourceCustomerId, String targetCustomerId) {
        if (!seedEnabled) {
            return;
        }
        if (sourceCustomerId == null || targetCustomerId == null || sourceCustomerId.equals(targetCustomerId)) {
            return;
        }
        int pools = 0;
        for (OverheadPool src : overheadPoolRepository.findByCustomerIdOrderByCodeAsc(sourceCustomerId)) {
            if (src.getStatus() != MasterDataStatus.ACTIVE) {
                continue;
            }
            if (overheadPoolRepository.existsByCustomerIdAndCode(targetCustomerId, src.getCode())) {
                continue;
            }
            OverheadPool copy = new OverheadPool();
            copy.setRecordId(UUID.randomUUID().toString());
            copy.setCustomerId(targetCustomerId);
            copy.setCode(src.getCode());
            copy.setDescription(src.getDescription());
            copy.setPoolCategory(src.getPoolCategory());
            copy.setAllocationBase(src.getAllocationBase());
            copy.setUom(src.getUom());
            copy.setPlantId(null); // global within tenant until plant mapped
            copy.setEffectiveFrom(src.getEffectiveFrom());
            copy.setEffectiveTo(src.getEffectiveTo());
            copy.setPeriod(src.getPeriod());
            copy.setVersionNumber(1);
            copy.setRate(src.getRate());
            copy.setStatus(MasterDataStatus.ACTIVE);
            overheadPoolRepository.save(copy);
            pools++;
        }

        int questions = 0;
        for (VendorQuestion src : vendorQuestionRepository
                .findByCustomerIdAndActiveFlagTrueOrderByCategoryAscQuestionCodeAsc(sourceCustomerId)) {
            if (vendorQuestionRepository.existsByCustomerIdAndQuestionCodeAndVersionNo(
                    targetCustomerId, src.getQuestionCode(), 1)) {
                continue;
            }
            VendorQuestion q = new VendorQuestion();
            q.setQuestionId(UUID.randomUUID().toString());
            q.setCustomerId(targetCustomerId);
            q.setQuestionCode(src.getQuestionCode());
            q.setVersionNo(1);
            q.setCategory(src.getCategory());
            q.setQuestionText(src.getQuestionText());
            q.setAnswerType(src.getAnswerType());
            q.setRequiredFlag(src.isRequiredFlag());
            q.setEvidenceRequired(src.isEvidenceRequired());
            q.setMandatoryGate(src.isMandatoryGate());
            q.setWeight(src.getWeight());
            q.setScoringRule(src.getScoringRule());
            q.setActiveFlag(true);
            q.setEffectiveFrom(src.getEffectiveFrom());
            q.setEffectiveTo(src.getEffectiveTo());
            vendorQuestionRepository.save(q);
            questions++;
        }
        log.info("Tenant catalog seed target={} overheadPools={} questions={}", targetCustomerId, pools, questions);
    }
}
