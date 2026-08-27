package com.automotive.costing.vendorselection.service;


import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.rfq.entity.SupplierMaster;
import com.automotive.costing.rfq.entity.SupplierStatus;
import com.automotive.costing.rfq.service.SupplierMasterService;
import com.automotive.costing.vendorselection.dto.VendorSelectionDtos.CreateQualityMetricRequest;
import com.automotive.costing.vendorselection.dto.VendorSelectionDtos.VerifyEvidenceRequest;
import com.automotive.costing.vendorselection.repository.*;
import com.automotive.costing.vendorselection.entity.EvidenceStatus;
import com.automotive.costing.vendorselection.entity.QualityMetricType;
import com.automotive.costing.vendorselection.entity.SupplierQualityMetric;
import com.automotive.costing.vendorselection.repository.SupplierCapabilityRepository;
import com.automotive.costing.vendorselection.repository.SupplierDeliveryMetricRepository;
import com.automotive.costing.vendorselection.repository.SupplierGuaranteeTermRepository;
import com.automotive.costing.vendorselection.repository.SupplierPolicyEvidenceRepository;
import com.automotive.costing.vendorselection.repository.SupplierQualityMetricRepository;
import com.automotive.costing.vendorselection.repository.SupplierWarrantyTermRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import com.automotive.costing.vendorselection.engine.EvidenceStatusTransitionEngine;
import com.automotive.costing.vendorselection.engine.WarrantyClaimStatusTransitionEngine;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierEvidenceServiceTest {

    @Mock private SupplierQualityMetricRepository qualityRepository;
    @Mock private SupplierDeliveryMetricRepository deliveryRepository;
    @Mock private SupplierWarrantyTermRepository warrantyRepository;
    @Mock private SupplierWarrantyClaimRepository warrantyClaimRepository;
    @Mock private SupplierGuaranteeTermRepository guaranteeRepository;
    @Mock private SupplierGuaranteeEventRepository guaranteeEventRepository;
    @Mock private SupplierAfterSalesMetricRepository afterSalesRepository;
    @Mock private SupplierPerformanceFeedbackRepository performanceFeedbackRepository;
    @Mock private SupplierCapabilityRepository capabilityRepository;
    @Mock private SupplierPolicyEvidenceRepository policyRepository;
    @Mock private SupplierMasterService supplierMasterService;
    @Mock private AuditService auditService;
    @Spy private EvidenceStatusTransitionEngine evidenceStatusTransitionEngine = new EvidenceStatusTransitionEngine();
    @Spy private WarrantyClaimStatusTransitionEngine claimStatusTransitionEngine = new WarrantyClaimStatusTransitionEngine();

    
    @Mock private TenantAccessGuard tenantAccessGuard;
    @org.junit.jupiter.api.BeforeEach
    void stubTenantAccessGuard() {
        org.mockito.Mockito.lenient().when(tenantAccessGuard.requireCustomerId()).thenReturn("cust-test-1");
    }
@InjectMocks
    private SupplierEvidenceService service;

    @Test
    void addQuality_startsPendingNotVerified() {
        SupplierMaster s = new SupplierMaster();
        s.setSupplierId("sup-1");
        s.setStatus(SupplierStatus.ACTIVE);
        when(supplierMasterService.require("sup-1")).thenReturn(s);
        when(qualityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resp = service.addQuality(new CreateQualityMetricRequest(
                "sup-1", QualityMetricType.PPM,
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31),
                new BigDecimal("12.5"), "ppm", 10000, null, null));

        assertThat(resp.evidenceStatus()).isEqualTo(EvidenceStatus.PENDING);
        assertThat(resp.verifiedAt()).isNull();
    }

    @Test
    void verifyQuality_setsVerifiedStatus() {
        SupplierQualityMetric m = new SupplierQualityMetric();
        m.setMetricId("m1");
        m.setSupplierId("sup-1");
        m.setMetricType(QualityMetricType.PPM);
        m.setPeriodStart(LocalDate.of(2025, 1, 1));
        m.setPeriodEnd(LocalDate.of(2025, 12, 31));
        m.setMetricValue(new BigDecimal("10"));
        m.setEvidenceStatus(EvidenceStatus.PENDING);
        when(qualityRepository.findByCustomerIdAndMetricId("cust-test-1", "m1")).thenReturn(Optional.of(m));
        when(qualityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resp = service.verifyQuality("m1", new VerifyEvidenceRequest(EvidenceStatus.VERIFIED, "doc-1"));
        assertThat(resp.evidenceStatus()).isEqualTo(EvidenceStatus.VERIFIED);
        assertThat(resp.evidenceRef()).isEqualTo("doc-1");
    }
}
