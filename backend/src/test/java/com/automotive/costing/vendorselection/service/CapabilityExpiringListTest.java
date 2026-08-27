package com.automotive.costing.vendorselection.service;


import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.rfq.service.SupplierMasterService;
import com.automotive.costing.vendorselection.engine.EligibilityGateEngine;
import com.automotive.costing.vendorselection.entity.CapabilityStatus;
import com.automotive.costing.vendorselection.entity.CapabilityType;
import com.automotive.costing.vendorselection.entity.SupplierCapability;
import com.automotive.costing.vendorselection.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CapabilityExpiringListTest {

    @Mock private SupplierCapabilityRepository capabilityRepository;
    @Mock private SupplierPolicyEvidenceRepository policyRepository;
    @Mock private VendorQuestionnaireEvaluationRepository questionnaireEvaluationRepository;
    @Mock private SupplierRiskRepository riskRepository;
    @Mock private SupplierQualificationCurrentRepository qualificationCurrentRepository;
    @Mock private SupplierEligibilityResultRepository eligibilityResultRepository;
    @Mock private SupplierMasterService supplierMasterService;
    @Mock private EligibilityGateEngine eligibilityGateEngine;
    @Mock private AuditService auditService;
    @Mock private ObjectMapper objectMapper;

    
    @Mock private TenantAccessGuard tenantAccessGuard;
    @org.junit.jupiter.api.BeforeEach
    void stubTenantAccessGuard() {
        org.mockito.Mockito.lenient().when(tenantAccessGuard.requireCustomerId()).thenReturn("cust-test-1");
    }
@InjectMocks private VendorCapabilityService service;

    @Test
    void listExpiring_returnsActivePastValidTo() {
        LocalDate asOf = LocalDate.of(2026, 8, 14);
        SupplierCapability c = new SupplierCapability();
        c.setCapabilityId("cap1");
        c.setSupplierId("S1");
        c.setCapabilityType(CapabilityType.PROCESS);
        c.setCapabilityCode("ISO-9001");
        c.setCapabilityName("ISO 9001");
        c.setStatus(CapabilityStatus.ACTIVE);
        c.setCustomerId("cust-test-1");
        c.setValidTo(LocalDate.of(2026, 8, 1));
        c.setCertified(true);
        when(capabilityRepository.findByStatusAndValidToLessThanEqualOrderByValidToAsc(CapabilityStatus.ACTIVE, asOf))
                .thenReturn(List.of(c));

        var list = service.listExpiring(asOf);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).capabilityCode()).isEqualTo("ISO-9001");
    }
}
