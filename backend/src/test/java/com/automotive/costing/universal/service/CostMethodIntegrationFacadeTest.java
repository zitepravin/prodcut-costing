package com.automotive.costing.universal.service;
import com.automotive.costing.universal.domain.CostMethodType;
import com.automotive.costing.universal.dto.UniversalCostingDtos.*;
import com.automotive.costing.universal.engine.AbcAllocationEngine;
import com.automotive.costing.universal.engine.UniversalCostCalculationEngine;
import org.junit.jupiter.api.BeforeEach; import org.junit.jupiter.api.Test;
import java.math.BigDecimal; import java.time.LocalDate; import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class CostMethodIntegrationFacadeTest {
  CostMethodIntegrationFacade facade;
  @BeforeEach void setUp() {
    facade = new CostMethodIntegrationFacade(new AbcAllocationEngine(), new UniversalCostCalculationEngine(new AbcAllocationEngine()));
  }
  @Test void should_uses_ShouldCostEngine() {
    var req = new CalculateRequest("obj1","SHOULD",LocalDate.now(),null,BigDecimal.ONE,null,null,
      null,null,null,null,
      new ShouldEngineInput(new BigDecimal("100"),new BigDecimal("50"),new BigDecimal("10"),
        BigDecimal.ZERO,new BigDecimal("20"),new BigDecimal("0.10"),new BigDecimal("0.05")), null,null);
    var result = facade.integrate(CostMethodType.SHOULD, req, "USD");
    assertEquals(0, new BigDecimal("180.000000").compareTo(result.technicalTotal()));
  }
  @Test void process_uses_ProcessCostEngine() {
    var req = new CalculateRequest("obj1","PROCESS",LocalDate.now(),null,BigDecimal.ONE,null,null,
      null,null,null,
      new ProcessEngineInput("OP1",new BigDecimal("60"),new BigDecimal("100"),new BigDecimal("50"),new BigDecimal("10"),new BigDecimal("100")),
      null,null,null);
    var result = facade.integrate(CostMethodType.PROCESS, req, "USD");
    assertTrue(result.technicalTotal().compareTo(BigDecimal.ZERO) > 0);
  }
  @Test void material_uses_MaterialCostEngine() {
    var req = new CalculateRequest("obj1","STANDARD",LocalDate.now(),null,BigDecimal.ONE,null,null,
      null,null, new MaterialEngineInput(new BigDecimal("2.0"),new BigDecimal("0.10"),new BigDecimal("5.00"),"USD"),
      null,null,null,null);
    var result = facade.integrate(CostMethodType.STANDARD, req, "USD");
    assertTrue(result.technicalTotal().compareTo(new BigDecimal("11")) > 0);
  }
  @Test void abc_uses_AbcAllocationEngine() {
    var req = new CalculateRequest("obj1","ABC",LocalDate.now(),null,BigDecimal.ONE,null,null,
      List.of(new ComponentInput("MATERIAL",new BigDecimal("40"),null,null,null,null)),
      List.of(new AbcPoolUsageInput("SETUP",new BigDecimal("10000"),new BigDecimal("1000"),new BigDecimal("50"))),
      null,null,null,null,null);
    var result = facade.integrate(CostMethodType.ABC, req, "USD");
    // 40 direct + 500 allocated = 540 (allocation applied once)
    assertEquals(0, new BigDecimal("540.000000").compareTo(result.technicalTotal()));
  }
  @Test void standard_from_direct_components() {
    var req = new CalculateRequest("obj1","STANDARD",LocalDate.now(),null,new BigDecimal("10"),null,null,
      List.of(new ComponentInput("MATERIAL",new BigDecimal("100"),null,null,null,null),
              new ComponentInput("LABOR",new BigDecimal("50"),null,null,null,null)),
      null,null,null,null,null,null);
    var result = facade.integrate(CostMethodType.STANDARD, req, "USD");
    assertEquals(0, new BigDecimal("150.000000").compareTo(result.technicalTotal()));
  }
}
