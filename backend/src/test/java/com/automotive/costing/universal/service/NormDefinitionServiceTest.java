package com.automotive.costing.universal.service;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.universal.dto.UniversalCostingDtos.NormRequest;
import com.automotive.costing.universal.entity.NormDefinition;
import com.automotive.costing.universal.repository.NormDefinitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class NormDefinitionServiceTest {
  private static final String TENANT = "cust-1";
  @Mock NormDefinitionRepository repository;
  @Mock TenantAccessGuard tenantAccessGuard;
  NormDefinitionService service;
  @BeforeEach void setUp() {
    service = new NormDefinitionService(repository, tenantAccessGuard);
    lenient().when(tenantAccessGuard.requireCustomerId()).thenReturn(TENANT);
  }
  @Test void create_and_resolve_effective() {
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    var resp = service.create(new NormRequest("SCRAP_PCT","Scrap","%","scrap",null,
        new BigDecimal("0.05"),null,null,LocalDate.of(2026,1,1),null,"MASTER"));
    assertEquals("SCRAP_PCT", resp.normCode());
    assertEquals("DRAFT", resp.approvalStatus());
    NormDefinition approved = new NormDefinition();
    approved.setNormCode("SCRAP_PCT"); approved.setNormValue(new BigDecimal("0.05"));
    approved.setApprovalStatus("APPROVED"); approved.setEffectiveFrom(LocalDate.of(2026,1,1));
    when(repository.findEffective(TENANT,"SCRAP_PCT",LocalDate.of(2026,6,1))).thenReturn(List.of(approved));
    assertTrue(service.resolveEffective("SCRAP_PCT", LocalDate.of(2026,6,1)).isPresent());
  }
  @Test void create_missing_code_rejected() {
    assertThrows(BusinessRuleViolationException.class, () ->
      service.create(new NormRequest(null,null,null,null,null,BigDecimal.ONE,null,null,LocalDate.now(),null,null)));
  }
  @Test void approve_sets_status() {
    NormDefinition n = new NormDefinition();
    n.setNormId("n1"); n.setCustomerId(TENANT); n.setApprovalStatus("DRAFT");
    when(repository.findByCustomerIdAndNormId(TENANT,"n1")).thenReturn(Optional.of(n));
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    assertEquals("APPROVED", service.approve("n1","user-1").approvalStatus());
  }
  @Test void createNewVersion_increments_version() {
    NormDefinition prior = new NormDefinition();
    prior.setNormId("n1"); prior.setCustomerId(TENANT); prior.setNormCode("SCRAP_PCT");
    prior.setVersionNo(1); prior.setApprovalStatus("APPROVED");
    prior.setEffectiveFrom(LocalDate.of(2026,1,1)); prior.setNormValue(new BigDecimal("0.05"));
    when(repository.findByCustomerIdAndNormId(TENANT, "n1")).thenReturn(Optional.of(prior));
    when(repository.findMaxVersionNo(TENANT, "SCRAP_PCT")).thenReturn(1);
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    var resp = service.createNewVersion("n1", new NormRequest(
        "SCRAP_PCT", "Scrap v2", "%", "scrap", null, new BigDecimal("0.08"),
        null, null, LocalDate.of(2026,7,1), null, "MASTER"));
    assertEquals(2, resp.versionNo());
    assertEquals("DRAFT", resp.approvalStatus());
    assertEquals(0, new BigDecimal("0.08").compareTo(resp.normValue()));
  }
  @Test void approved_norm_immutable_on_approve() {
    NormDefinition n = new NormDefinition();
    n.setNormId("n1"); n.setCustomerId(TENANT); n.setApprovalStatus("APPROVED");
    when(repository.findByCustomerIdAndNormId(TENANT, "n1")).thenReturn(Optional.of(n));
    assertThrows(BusinessRuleViolationException.class, () -> service.approve("n1", "u1"));
  }
}
