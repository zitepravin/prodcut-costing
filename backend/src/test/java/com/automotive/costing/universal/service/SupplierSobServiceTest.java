package com.automotive.costing.universal.service;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.universal.dto.UniversalCostingDtos.SobRequest;
import com.automotive.costing.universal.entity.SupplierShareOfBusiness;
import com.automotive.costing.universal.repository.SupplierShareOfBusinessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class SupplierSobServiceTest {
  private static final String TENANT = "cust-1";
  @Mock SupplierShareOfBusinessRepository repository;
  @Mock TenantAccessGuard tenantAccessGuard;
  SupplierSobService service;
  @BeforeEach void setUp() {
    service = new SupplierSobService(repository, tenantAccessGuard);
    lenient().when(tenantAccessGuard.requireCustomerId()).thenReturn(TENANT);
  }
  @Test void create_partial_group_fails_100() {
    when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    SupplierShareOfBusiness a = new SupplierShareOfBusiness();
    a.setAllocationPercent(new BigDecimal("60.0000")); a.setAllocationGroupCode("GRP1"); a.setStatus("ACTIVE");
    when(repository.findActiveForGroup(eq(TENANT), eq("GRP1"), any())).thenReturn(List.of(a));
    assertThrows(BusinessRuleViolationException.class, () ->
      service.create(new SobRequest(null,"P1","sup-A",new BigDecimal("60"),"VOLUME","GRP1",LocalDate.of(2026,1,1),null,1)));
  }
  @Test void validateGroupTotal_exactly_100_passes() {
    SupplierShareOfBusiness a = new SupplierShareOfBusiness(); a.setAllocationPercent(new BigDecimal("60.0000"));
    SupplierShareOfBusiness b = new SupplierShareOfBusiness(); b.setAllocationPercent(new BigDecimal("40.0000"));
    when(repository.findActiveForGroup(TENANT,"GRP1",LocalDate.of(2026,6,1))).thenReturn(List.of(a,b));
    assertDoesNotThrow(() -> service.validateGroupTotal(TENANT,"GRP1",LocalDate.of(2026,6,1)));
  }
  @Test void validateGroupTotal_not_100_fails() {
    SupplierShareOfBusiness a = new SupplierShareOfBusiness(); a.setAllocationPercent(new BigDecimal("50.0000"));
    when(repository.findActiveForGroup(TENANT,"GRP1",LocalDate.of(2026,6,1))).thenReturn(List.of(a));
    assertThrows(BusinessRuleViolationException.class, () -> service.validateGroupTotal(TENANT,"GRP1",LocalDate.of(2026,6,1)));
  }
  @Test void invalid_percent_rejected() {
    assertThrows(BusinessRuleViolationException.class, () ->
      service.create(new SobRequest(null,null,"sup",new BigDecimal("0"),"VOLUME","G",LocalDate.now(),null,1)));
  }
  @Test void partial_policy_allows_under_100() {
    SupplierShareOfBusiness a = new SupplierShareOfBusiness();
    a.setAllocationPercent(new BigDecimal("60.0000"));
    when(repository.findActiveForGroup(TENANT, "GRP1", LocalDate.of(2026, 6, 1))).thenReturn(List.of(a));
    assertDoesNotThrow(() -> service.validateGroupTotal(TENANT, "GRP1", LocalDate.of(2026, 6, 1), true));
  }
  @Test void partial_policy_rejects_over_100() {
    SupplierShareOfBusiness a = new SupplierShareOfBusiness();
    a.setAllocationPercent(new BigDecimal("60.0000"));
    SupplierShareOfBusiness b = new SupplierShareOfBusiness();
    b.setAllocationPercent(new BigDecimal("50.0000"));
    when(repository.findActiveForGroup(TENANT, "GRP1", LocalDate.of(2026, 6, 1))).thenReturn(List.of(a, b));
    assertThrows(BusinessRuleViolationException.class, () ->
        service.validateGroupTotal(TENANT, "GRP1", LocalDate.of(2026, 6, 1), true));
  }
}
