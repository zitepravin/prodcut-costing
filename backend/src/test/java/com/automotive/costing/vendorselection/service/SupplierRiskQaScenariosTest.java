package com.automotive.costing.vendorselection.service;


import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.rfq.service.SupplierMasterService;
import com.automotive.costing.vendorselection.engine.RiskStatusTransitionEngine;
import com.automotive.costing.vendorselection.entity.RiskSeverity;
import com.automotive.costing.vendorselection.entity.RiskStatus;
import com.automotive.costing.vendorselection.entity.SupplierRisk;
import com.automotive.costing.vendorselection.repository.SupplierRiskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierRiskQaScenariosTest {

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
    @DisplayName("QA: Accept risk without mitigation plan is rejected")
    void acceptRequiresMitigation() {
        SupplierRisk r = new SupplierRisk();
        r.setRiskId("r1");
        r.setStatus(RiskStatus.OPEN);
        when(riskRepository.findByCustomerIdAndRiskId("cust-test-1", "r1")).thenReturn(Optional.of(r));

        assertThatThrownBy(() -> service.updateStatus("r1",
                new SupplierRiskService.UpdateRiskStatusRequest(RiskStatus.ACCEPTED, null, null, null)))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("QA: OPEN to MITIGATING transitions and audits")
    void openToMitigating() {
        SupplierRisk r = new SupplierRisk();
        r.setRiskId("r1");
        r.setStatus(RiskStatus.OPEN);
        r.setSeverity(RiskSeverity.HIGH);
        when(riskRepository.findByCustomerIdAndRiskId("cust-test-1", "r1")).thenReturn(Optional.of(r));
        when(riskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resp = service.updateStatus("r1",
                new SupplierRiskService.UpdateRiskStatusRequest(RiskStatus.MITIGATING, "plan", null, null));
        assertThat(resp.status()).isEqualTo(RiskStatus.MITIGATING);
        verify(auditService).recordTransition(eq("SupplierRisk"), eq("r1"), eq("OPEN"), eq("MITIGATING"));
    }
}
