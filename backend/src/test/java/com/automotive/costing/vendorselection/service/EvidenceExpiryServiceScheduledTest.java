package com.automotive.costing.vendorselection.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.vendorselection.entity.EvidenceExpiryJobStatus;
import com.automotive.costing.vendorselection.repository.EvidenceExpiryJobStatusRepository;
import com.automotive.costing.vendorselection.repository.EvidenceExpiryRunRepository;
import com.automotive.costing.vendorselection.repository.SupplierGuaranteeTermRepository;
import com.automotive.costing.vendorselection.repository.SupplierPolicyEvidenceRepository;
import com.automotive.costing.vendorselection.repository.SupplierWarrantyTermRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class EvidenceExpiryServiceScheduledTest {

    @Mock private SupplierPolicyEvidenceRepository policyRepository;
    @Mock private SupplierWarrantyTermRepository warrantyRepository;
    @Mock private SupplierGuaranteeTermRepository guaranteeRepository;
    @Mock private EvidenceExpiryRunRepository runRepository;
    @Mock private EvidenceExpiryJobStatusRepository jobStatusRepository;
    @Mock private AuditService auditService;

    @InjectMocks private EvidenceExpiryService service;

    @Test
    void runScheduledPass_whenDisabled_skipsExpiry() {
        EvidenceExpiryJobStatus status = new EvidenceExpiryJobStatus();
        status.setStatusId(UUID.randomUUID().toString());
        status.setJobName(EvidenceExpiryService.JOB_NAME);
        status.setEnabledFlag(false);
        status.setUpdatedAt(Instant.now());
        when(jobStatusRepository.findByJobName(EvidenceExpiryService.JOB_NAME)).thenReturn(Optional.of(status));
        when(jobStatusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.runScheduledPass();
        assertThat(result).isNull();
        verify(jobStatusRepository).save(any());
    }

    @Test
    void runScheduledPass_whenEnabled_writesRun() {
        EvidenceExpiryJobStatus status = new EvidenceExpiryJobStatus();
        status.setStatusId(UUID.randomUUID().toString());
        status.setJobName(EvidenceExpiryService.JOB_NAME);
        status.setEnabledFlag(true);
        status.setUpdatedAt(Instant.now());
        when(jobStatusRepository.findByJobName(EvidenceExpiryService.JOB_NAME)).thenReturn(Optional.of(status));
        when(jobStatusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(policyRepository.findAll()).thenReturn(List.of());
        when(warrantyRepository.findAll()).thenReturn(List.of());
        when(guaranteeRepository.findAll()).thenReturn(List.of());
        when(runRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.runScheduledPass();
        assertThat(result).isNotNull();
        assertThat(result.triggerSource()).isEqualTo("SCHEDULED");
    }
}
