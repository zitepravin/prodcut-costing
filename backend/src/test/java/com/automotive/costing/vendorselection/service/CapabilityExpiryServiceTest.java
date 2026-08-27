package com.automotive.costing.vendorselection.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.vendorselection.entity.CapabilityStatus;
import com.automotive.costing.vendorselection.entity.SupplierCapability;
import com.automotive.costing.vendorselection.repository.CapabilityExpiryJobStatusRepository;
import com.automotive.costing.vendorselection.repository.CapabilityExpiryRunRepository;
import com.automotive.costing.vendorselection.repository.SupplierCapabilityRepository;
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
class CapabilityExpiryServiceTest {

    @Mock private SupplierCapabilityRepository capabilityRepository;
    @Mock private CapabilityExpiryRunRepository runRepository;
    @Mock private CapabilityExpiryJobStatusRepository jobStatusRepository;
    @Mock private AuditService auditService;

    @InjectMocks private CapabilityExpiryService service;

    @Test
    void expiresActivePastValidTo() {
        SupplierCapability c = new SupplierCapability();
        c.setCapabilityId("c1");
        c.setStatus(CapabilityStatus.ACTIVE);
        c.setValidTo(LocalDate.of(2020, 1, 1));
        when(capabilityRepository.findByStatus(CapabilityStatus.ACTIVE)).thenReturn(List.of(c));
        when(capabilityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(runRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.runExpiryPass(LocalDate.of(2026, 8, 13));
        assertThat(result.capabilitiesExpired()).isEqualTo(1);
        assertThat(c.getStatus()).isEqualTo(CapabilityStatus.EXPIRED);
    }

    @Test
    void doesNotExpireFutureValidTo() {
        SupplierCapability c = new SupplierCapability();
        c.setCapabilityId("c2");
        c.setStatus(CapabilityStatus.ACTIVE);
        c.setValidTo(LocalDate.of(2030, 1, 1));
        when(capabilityRepository.findByStatus(CapabilityStatus.ACTIVE)).thenReturn(List.of(c));
        when(runRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = service.runExpiryPass(LocalDate.of(2026, 8, 13));
        assertThat(result.capabilitiesExpired()).isEqualTo(0);
        assertThat(c.getStatus()).isEqualTo(CapabilityStatus.ACTIVE);
    }
}
