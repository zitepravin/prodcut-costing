package com.automotive.costing.vendorselection.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.vendorselection.entity.EvidenceStatus;
import com.automotive.costing.vendorselection.entity.SupplierPolicyEvidence;
import com.automotive.costing.vendorselection.entity.SupplierWarrantyTerm;
import com.automotive.costing.vendorselection.repository.EvidenceExpiryRunRepository;
import com.automotive.costing.vendorselection.repository.EvidenceExpiryJobStatusRepository;
import com.automotive.costing.vendorselection.repository.SupplierGuaranteeTermRepository;
import com.automotive.costing.vendorselection.repository.SupplierPolicyEvidenceRepository;
import com.automotive.costing.vendorselection.repository.SupplierWarrantyTermRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class EvidenceExpiryServiceTest {

    @Mock private SupplierPolicyEvidenceRepository policyRepository;
    @Mock private SupplierWarrantyTermRepository warrantyRepository;
    @Mock private SupplierGuaranteeTermRepository guaranteeRepository;
    @Mock private EvidenceExpiryRunRepository runRepository;
    @Mock private EvidenceExpiryJobStatusRepository jobStatusRepository;
    @Mock private AuditService auditService;

    @InjectMocks private EvidenceExpiryService service;

    @Test
    void expiresPastPolicyEvidence() {
        SupplierPolicyEvidence p = new SupplierPolicyEvidence();
        p.setPolicyId("p1");
        p.setEvidenceStatus(EvidenceStatus.VERIFIED);
        p.setExpiryDate(LocalDate.of(2020, 1, 1));
        when(policyRepository.findByEvidenceStatusIn(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(p));
        when(warrantyRepository.findByEvidenceStatusIn(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(guaranteeRepository.findByEvidenceStatusIn(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(policyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(runRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.runExpiryPass(LocalDate.of(2026, 8, 13));
        assertThat(result.policiesExpired()).isEqualTo(1);
        assertThat(p.getEvidenceStatus()).isEqualTo(EvidenceStatus.EXPIRED);
    }

    @Test
    void doesNotExpireFuturePolicy() {
        SupplierPolicyEvidence p = new SupplierPolicyEvidence();
        p.setPolicyId("p2");
        p.setEvidenceStatus(EvidenceStatus.VERIFIED);
        p.setExpiryDate(LocalDate.of(2030, 1, 1));
        when(policyRepository.findByEvidenceStatusIn(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(p));
        when(warrantyRepository.findByEvidenceStatusIn(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(guaranteeRepository.findByEvidenceStatusIn(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(runRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.runExpiryPass(LocalDate.of(2026, 8, 13));
        assertThat(result.policiesExpired()).isEqualTo(0);
        assertThat(p.getEvidenceStatus()).isEqualTo(EvidenceStatus.VERIFIED);
    }

    @Test
    void expiresPastWarranty() {
        SupplierWarrantyTerm w = new SupplierWarrantyTerm();
        w.setWarrantyId("w1");
        w.setEvidenceStatus(EvidenceStatus.VERIFIED);
        w.setEffectiveTo(LocalDate.of(2019, 6, 1));
        when(policyRepository.findByEvidenceStatusIn(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(warrantyRepository.findByEvidenceStatusIn(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(w));
        when(guaranteeRepository.findByEvidenceStatusIn(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(warrantyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(runRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.runExpiryPass(LocalDate.of(2026, 8, 13));
        assertThat(result.warrantiesExpired()).isEqualTo(1);
        assertThat(w.getEvidenceStatus()).isEqualTo(EvidenceStatus.EXPIRED);
    }
}
