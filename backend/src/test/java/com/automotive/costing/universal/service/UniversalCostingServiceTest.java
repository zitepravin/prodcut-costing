package com.automotive.costing.universal.service;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.universal.dto.UniversalCostingDtos.*;
import com.automotive.costing.universal.engine.AbcAllocationEngine;
import com.automotive.costing.universal.engine.UniversalCostCalculationEngine;
import com.automotive.costing.universal.entity.CostObject;
import com.automotive.costing.universal.entity.UniversalCostCalculation;
import com.automotive.costing.universal.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach; import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock; import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal; import java.time.LocalDate; import java.util.List; import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class UniversalCostingServiceTest {
  private static final String TENANT = "cust-001";
  @Mock CostObjectRepository costObjectRepository;
  @Mock CostDriverRepository costDriverRepository;
  @Mock CostPoolRepository costPoolRepository;
  @Mock UniversalCostCalculationRepository calculationRepository;
  @Mock IndustryCostingProfileRepository profileRepository;
  @Mock NormDefinitionService normDefinitionService;
  @Mock SupplierSobService supplierSobService;
  @Mock TenantAccessGuard tenantAccessGuard;
  UniversalCostingService service;
  @BeforeEach void setUp() {
    var abc = new AbcAllocationEngine();
    var engine = new UniversalCostCalculationEngine(abc);
    var facade = new CostMethodIntegrationFacade(abc, engine);
    service = new UniversalCostingService(costObjectRepository, costDriverRepository, costPoolRepository,
      calculationRepository, profileRepository, facade, normDefinitionService, supplierSobService,
      tenantAccessGuard, new ObjectMapper());
    lenient().when(tenantAccessGuard.requireCustomerId()).thenReturn(TENANT);
  }
  @Test void calculate_with_commercial_layer_separate() {
    CostObject obj = new CostObject(); obj.setCostObjectId("obj-1"); obj.setCustomerId(TENANT); obj.setCurrencyCode("USD");
    when(costObjectRepository.findByCustomerIdAndCostObjectId(TENANT,"obj-1")).thenReturn(Optional.of(obj));
    when(calculationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    CalculateResponse resp = service.calculate(new CalculateRequest(
      "obj-1","STANDARD",LocalDate.now(),null,new BigDecimal("1"),null,null,
      List.of(new ComponentInput("MATERIAL",new BigDecimal("100"),null,null,null,null)),
      null,null,null,null,
      new CommercialInput(new BigDecimal("10"),BigDecimal.ZERO,new BigDecimal("5"),BigDecimal.ZERO,new BigDecimal("20")), null));
    assertEquals(0, new BigDecimal("100.000000").compareTo(resp.technicalTotal()));
    assertEquals(0, new BigDecimal("15.000000").compareTo(resp.commercialTotal()));
    assertEquals(0, new BigDecimal("115.000000").compareTo(resp.totalCost()));
    assertEquals("DRAFT", resp.lifecycleStatus());
  }
  @Test void lifecycle_draft_submit_approve_lock() {
    UniversalCostCalculation calc = new UniversalCostCalculation();
    calc.setCalculationId("c1"); calc.setCustomerId(TENANT); calc.setLifecycleStatus("DRAFT"); calc.setTotalCost(BigDecimal.TEN);
    when(calculationRepository.findByCustomerIdAndCalculationId(TENANT,"c1")).thenReturn(Optional.of(calc));
    when(calculationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    service.transition("c1", new LifecycleTransitionRequest("SUBMIT","u1"));
    assertEquals("SUBMITTED", calc.getLifecycleStatus());
    service.transition("c1", new LifecycleTransitionRequest("APPROVE","u1"));
    assertEquals("APPROVED", calc.getLifecycleStatus());
    service.transition("c1", new LifecycleTransitionRequest("LOCK","u1"));
    assertEquals("LOCKED", calc.getLifecycleStatus());
    assertNotNull(calc.getLockedAt());
  }
  @Test void freeze_blocks_mutation() {
    UniversalCostCalculation calc = new UniversalCostCalculation();
    calc.setLifecycleStatus("LOCKED");
    assertThrows(BusinessRuleViolationException.class, () -> service.assertMutable(calc));
  }
  @Test void invalid_method_rejected() {
    CostObject obj = new CostObject(); obj.setCostObjectId("obj-1"); obj.setCustomerId(TENANT); obj.setCurrencyCode("USD");
    when(costObjectRepository.findByCustomerIdAndCostObjectId(TENANT,"obj-1")).thenReturn(Optional.of(obj));
    assertThrows(BusinessRuleViolationException.class, () ->
      service.calculate(new CalculateRequest("obj-1","NOT_A_METHOD",LocalDate.now(),null,null,null,null,
        List.of(),null,null,null,null,null,null)));
  }
  @Test void createCostObject_success() {
    when(costObjectRepository.existsByCustomerIdAndObjectCode(TENANT,"JOB-1")).thenReturn(false);
    when(costObjectRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    var r = service.createCostObject(new CostObjectRequest("JOB","JOB-1","Job One","INR","EA",null));
    assertEquals("JOB", r.objectType());
    assertEquals(TENANT, r.customerId());
  }
}
