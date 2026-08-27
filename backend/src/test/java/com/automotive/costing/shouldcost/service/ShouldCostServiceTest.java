package com.automotive.costing.shouldcost.service;

import com.automotive.costing.bom.entity.BomNode;
import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.versioning.service.CostVersionService;
import com.automotive.costing.platform.user.service.AccessScopeService;
import com.automotive.costing.platform.user.service.AccessScopeService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.masterdata.entity.VehicleProgram;
import com.automotive.costing.masterdata.repository.OverheadPoolRepository;
import com.automotive.costing.masterdata.repository.VehicleProgramRepository;
import com.automotive.costing.material.repository.MaterialCostCalculationRepository;
import com.automotive.costing.process.repository.ProcessCostCalculationRepository;
import com.automotive.costing.shouldcost.dto.ShouldCostDtos.GenerateRequest;
import com.automotive.costing.shouldcost.dto.ShouldCostDtos.RegionalCompareRequest;
import com.automotive.costing.shouldcost.dto.ShouldCostDtos.Response;
import com.automotive.costing.shouldcost.dto.ShouldCostDtos.VarianceRequest;
import com.automotive.costing.shouldcost.entity.SgaMarginBenchmark;
import com.automotive.costing.shouldcost.entity.ShouldCostBuildUp;
import com.automotive.costing.shouldcost.repository.ParametricCostBenchmarkRepository;
import com.automotive.costing.shouldcost.repository.SgaMarginBenchmarkRepository;
import com.automotive.costing.shouldcost.repository.ShouldCostBuildUpRepository;
import com.automotive.costing.emissions.service.ShouldCostEmissionsService;
import com.automotive.costing.shouldcost.repository.CostComponentDefinitionRepository;
import com.automotive.costing.shouldcost.repository.ShouldCostQuoteComponentRepository;
import com.automotive.costing.tooling.repository.ToolingCostCalculationRepository;
import com.automotive.costing.currency.service.CostCurrencyConverter;
import com.automotive.costing.packaging.repository.PackagingLogisticsCostCalculationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShouldCostServiceTest {

    @Mock private BomNodeRepository bomNodeRepository;
    @Mock private AccessScopeService accessScopeService;
    @Mock private VehicleProgramRepository vehicleProgramRepository;
    @Mock private MaterialCostCalculationRepository materialCostCalculationRepository;
    @Mock private ProcessCostCalculationRepository processCostCalculationRepository;
    @Mock private ToolingCostCalculationRepository toolingCostCalculationRepository;
    @Mock private OverheadPoolRepository overheadPoolRepository;
    @Mock private SgaMarginBenchmarkRepository sgaMarginBenchmarkRepository;
    @Mock private ParametricCostBenchmarkRepository parametricCostBenchmarkRepository;
    @Mock private ShouldCostBuildUpRepository buildUpRepository;
    @Mock private TenantAccessGuard tenantAccessGuard;
    @Mock private ShouldCostEmissionsService shouldCostEmissionsService;
    @Mock private AuditService auditService;
    @Mock
    private CostVersionService costVersionService;
    @Mock private CostCurrencyConverter costCurrencyConverter;
    @Mock private PackagingLogisticsCostCalculationRepository packagingLogisticsCostCalculationRepository;
    @Mock private CostComponentDefinitionRepository costComponentDefinitionRepository;
    @Mock private ShouldCostQuoteComponentRepository shouldCostQuoteComponentRepository;
    @Mock private com.automotive.costing.costingvariant.service.CostingVariantService costingVariantService;

    private ShouldCostService service;

    @BeforeEach
    void setUp() {
        // Identity conversion for existing tests (same currency / base == target)
        lenient().when(costVersionService.snapshotAfterChange(any(), any(), any(), any(), any(), any()))
                .thenReturn(null);
        lenient().when(costCurrencyConverter.toProgramCurrency(any(), any(), any()))
                .thenAnswer(inv -> inv.getArgument(0) == null ? BigDecimal.ZERO : inv.getArgument(0));
        lenient().when(costCurrencyConverter.convertToProgramCurrency(any(), any(), any()))
                .thenAnswer(inv -> {
                    java.math.BigDecimal amt = inv.getArgument(0) == null ? BigDecimal.ZERO : inv.getArgument(0);
                    String target = inv.getArgument(1);
                    java.time.LocalDate asOf = inv.getArgument(2);
                    return new com.automotive.costing.currency.service.CostCurrencyConverter.FxConversionResult(
                            amt, "USD", amt, target != null ? target : "USD", BigDecimal.ONE, asOf, asOf, "IDENTITY", null);
                });
        lenient().when(tenantAccessGuard.requireCustomerId()).thenReturn("cust-1");
        lenient().doNothing().when(accessScopeService).assertAccessForCurrentUser(any(), any());
        lenient().when(packagingLogisticsCostCalculationRepository
                .findFirstByBomNodeIdOrderByCalculatedAtDesc(any()))
                .thenReturn(java.util.Optional.empty());
        service = new ShouldCostService(bomNodeRepository, accessScopeService, vehicleProgramRepository,
                materialCostCalculationRepository, processCostCalculationRepository,
                toolingCostCalculationRepository, overheadPoolRepository, sgaMarginBenchmarkRepository,
                parametricCostBenchmarkRepository, buildUpRepository, tenantAccessGuard, shouldCostEmissionsService, auditService, costVersionService,
                costCurrencyConverter, packagingLogisticsCostCalculationRepository,
                costComponentDefinitionRepository, shouldCostQuoteComponentRepository, costingVariantService);
    }

    private BomNode bomNode() {
        BomNode node = new BomNode();
        node.setBomNodeId("node-1");
        node.setProgramId("program-1");
        return node;
    }

    // SRS 4.8.5 (Negative/Boundary): no region supplied and the program has none configured either -> blocked.
    @Test
    void generate_withNoRegionAvailable_throwsBusinessRuleViolation() {
        when(bomNodeRepository.findByCustomerIdAndBomNodeId("cust-1", "node-1")).thenReturn(Optional.of(bomNode()));
        VehicleProgram programWithoutRegion = new VehicleProgram();
        programWithoutRegion.setProgramId("program-1");
        when(vehicleProgramRepository.findByCustomerIdAndProgramId("cust-1", "program-1")).thenReturn(Optional.of(programWithoutRegion));

        GenerateRequest request = new GenerateRequest("node-1", null, "Stamped Steel", null, null, null, null, null);

        assertThatThrownBy(() -> service.generate(request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("region");
    }

    // SRS 4.8.5 (Negative/Boundary): region resolves (from program), but no benchmark is configured for it -> blocked, not defaulted.
    @Test
    void generate_withoutConfiguredSgaMarginBenchmark_throwsBusinessRuleViolation() {
        when(bomNodeRepository.findByCustomerIdAndBomNodeId("cust-1", "node-1")).thenReturn(Optional.of(bomNode()));
        VehicleProgram program = new VehicleProgram();
        program.setProgramId("program-1");
        program.setSourcingRegion("APAC");
        when(vehicleProgramRepository.findByCustomerIdAndProgramId("cust-1", "program-1")).thenReturn(Optional.of(program));
        when(materialCostCalculationRepository.findFirstByBomNodeIdOrderByCalculatedAtDesc("node-1"))
                .thenReturn(Optional.empty());
        when(processCostCalculationRepository.findFirstByBomNodeIdOrderByCalculatedAtDesc("node-1"))
                .thenReturn(Optional.empty());
        when(toolingCostCalculationRepository.findFirstByBomNodeIdOrderByCalculatedAtDesc("node-1"))
                .thenReturn(Optional.empty());
        when(sgaMarginBenchmarkRepository.findFirstByCommodityFamilyAndRegionAndActiveTrue("Stamped Steel", "APAC"))
                .thenReturn(Optional.empty());

        GenerateRequest request = new GenerateRequest("node-1", null, "Stamped Steel", null, null, null, null, null);

        assertThatThrownBy(() -> service.generate(request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("SRS 4.8.5");
    }

    // Positive path: region falls back to the program's sourcingRegion, benchmark resolves, defaults applied.
    @Test
    void generate_withRegionFromProgramAndConfiguredBenchmark_succeeds() {
        when(bomNodeRepository.findByCustomerIdAndBomNodeId("cust-1", "node-1")).thenReturn(Optional.of(bomNode()));
        VehicleProgram program = new VehicleProgram();
        program.setProgramId("program-1");
        program.setSourcingRegion("APAC");
        when(vehicleProgramRepository.findByCustomerIdAndProgramId("cust-1", "program-1")).thenReturn(Optional.of(program));
        when(materialCostCalculationRepository.findFirstByBomNodeIdOrderByCalculatedAtDesc("node-1"))
                .thenReturn(Optional.empty());
        when(processCostCalculationRepository.findFirstByBomNodeIdOrderByCalculatedAtDesc("node-1"))
                .thenReturn(Optional.empty());
        when(toolingCostCalculationRepository.findFirstByBomNodeIdOrderByCalculatedAtDesc("node-1"))
                .thenReturn(Optional.empty());

        SgaMarginBenchmark benchmark = new SgaMarginBenchmark();
        benchmark.setSgaPct(new BigDecimal("0.10"));
        benchmark.setMarginPct(new BigDecimal("0.08"));
        when(sgaMarginBenchmarkRepository.findFirstByCommodityFamilyAndRegionAndActiveTrue("Stamped Steel", "APAC"))
                .thenReturn(Optional.of(benchmark));
        when(buildUpRepository.save(any(ShouldCostBuildUp.class))).thenAnswer(inv -> inv.getArgument(0));

        GenerateRequest request = new GenerateRequest("node-1", null, "Stamped Steel", null, null, null, null, null);
        Response result = service.generate(request);

        assertThat(result.scenarioLabel()).isEqualTo("Current Design");
        assertThat(result.totalCost()).isEqualByComparingTo("0.000000");
        verify(auditService).recordCreate(eq("ShouldCostBuildUp"), anyString(), any());
    }

    // SC-04 (Negative/Boundary): cannot compute percentage variance against a zero should-cost total.
    @Test
    void varianceAgainstQuote_withZeroShouldCostTotal_throwsBusinessRuleViolation() {
        ShouldCostBuildUp buildUp = new ShouldCostBuildUp();
        buildUp.setBuildUpId("bu-1");
        buildUp.setTotalCost(BigDecimal.ZERO);
        when(buildUpRepository.findByCustomerIdAndBuildUpId("cust-1", "bu-1")).thenReturn(Optional.of(buildUp));

        VarianceRequest request = new VarianceRequest("bu-1", new BigDecimal("100"));

        assertThatThrownBy(() -> service.varianceAgainstQuote(request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("SC-04");
    }

    // SC-05 (Negative/Boundary): fewer than two build-ups cannot be compared.
    @Test
    void regionalCompare_withFewerThanTwoBuildUpIds_throwsBusinessRuleViolation() {
        RegionalCompareRequest request = new RegionalCompareRequest(List.of("bu-1"));

        assertThatThrownBy(() -> service.regionalCompare(request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("SC-05");
    }

    // WF-06/SRS 4.8.5 (Positive): locking sets both the locked flag AND approvalStatus=APPROVED.
    @Test
    void lock_setsLockedFlagAndApprovalStatusApproved() {
        com.automotive.costing.shouldcost.entity.ShouldCostBuildUp buildUp =
                new com.automotive.costing.shouldcost.entity.ShouldCostBuildUp();
        buildUp.setBuildUpId("bu-1");
        buildUp.setLocked(false);
        buildUp.setApprovalStatus(com.automotive.costing.shouldcost.entity.ApprovalStatus.SUBMITTED);
        when(buildUpRepository.findByCustomerIdAndBuildUpId("cust-1", "bu-1")).thenReturn(Optional.of(buildUp));
        when(buildUpRepository.save(any(com.automotive.costing.shouldcost.entity.ShouldCostBuildUp.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Response result = service.lock("bu-1");

        assertThat(result.locked()).isTrue();
        assertThat(result.approvalStatus()).isEqualTo("APPROVED");
    }

    // SRS 4.14.5 (Positive): a rejected workflow returns the build-up to Draft.
    @Test
    void markRejectedReturnToDraft_setsApprovalStatusToDraft() {
        com.automotive.costing.shouldcost.entity.ShouldCostBuildUp buildUp =
                new com.automotive.costing.shouldcost.entity.ShouldCostBuildUp();
        buildUp.setBuildUpId("bu-1");
        buildUp.setApprovalStatus(com.automotive.costing.shouldcost.entity.ApprovalStatus.SUBMITTED);
        when(buildUpRepository.findByCustomerIdAndBuildUpId("cust-1", "bu-1")).thenReturn(Optional.of(buildUp));
        when(buildUpRepository.save(any(com.automotive.costing.shouldcost.entity.ShouldCostBuildUp.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Response result = service.markRejectedReturnToDraft("bu-1");

        assertThat(result.approvalStatus()).isEqualTo("DRAFT");
    }
}
