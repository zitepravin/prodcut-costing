package com.automotive.costing.vendorselection.service;


import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.rfq.entity.SupplierMaster;
import com.automotive.costing.rfq.service.SupplierMasterService;
import com.automotive.costing.vendorselection.entity.RiskCategory;
import com.automotive.costing.vendorselection.entity.RiskSeverity;
import com.automotive.costing.vendorselection.entity.RiskStatus;
import com.automotive.costing.vendorselection.entity.SupplierRisk;
import com.automotive.costing.vendorselection.repository.SupplierRiskRepository;
import com.automotive.costing.vendorselection.service.SupplierRiskService.CreateRiskRequest;
import com.automotive.costing.vendorselection.service.SupplierRiskService.UpdateRiskStatusRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import com.automotive.costing.vendorselection.engine.RiskStatusTransitionEngine;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierRiskServiceTest {

    @Mock private SupplierRiskRepository riskRepository;
    @Mock private SupplierMasterService supplierMasterService;
    @Mock private AuditService auditService;
    @Spy private RiskStatusTransitionEngine riskStatusTransitionEngine = new RiskStatusTransitionEngine();
    
    @Mock private TenantAccessGuard tenantAccessGuard;
    @org.junit.jupiter.api.BeforeEach
    void stubTenantAccessGuard() {
        org.mockito.Mockito.lenient().when(tenantAccessGuard.requireCustomerId()).thenReturn("cust-test-1");
    }
@InjectMocks private SupplierRiskService service;

    @Test
    void create_startsOpen() {
        when(supplierMasterService.require("s1")).thenReturn(new SupplierMaster());
        when(riskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var r = service.create(new CreateRiskRequest(
                "s1", RiskCategory.QUALITY, "High PPM trend", null,
                RiskSeverity.HIGH, null, null, null));
        assertThat(r.status()).isEqualTo(RiskStatus.OPEN);
        assertThat(r.severity()).isEqualTo(RiskSeverity.HIGH);
    }

    @Test
    void acceptWithoutRationale_fails() {
        SupplierRisk risk = new SupplierRisk();
        risk.setRiskId("r1");
        risk.setStatus(RiskStatus.OPEN);
        when(riskRepository.findByCustomerIdAndRiskId("cust-test-1", "r1")).thenReturn(Optional.of(risk));
        assertThatThrownBy(() -> service.updateStatus("r1",
                new UpdateRiskStatusRequest(RiskStatus.ACCEPTED, null, null, null)))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
