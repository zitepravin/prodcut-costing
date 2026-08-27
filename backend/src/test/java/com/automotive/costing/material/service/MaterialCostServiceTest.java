package com.automotive.costing.material.service;

import com.automotive.costing.bom.entity.BomNode;
import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.bom.service.BomNodeService;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.material.dto.MaterialCostDtos.CalculateRequest;
import com.automotive.costing.material.dto.MaterialCostDtos.NetWeightSource;
import com.automotive.costing.material.dto.MaterialCostDtos.CompareRequest;
import com.automotive.costing.material.dto.MaterialCostDtos.HighMaterialCostCheckRequest;
import com.automotive.costing.material.dto.MaterialCostDtos.HighMaterialCostCheckResponse;
import com.automotive.costing.material.dto.MaterialCostDtos.Response;
import com.automotive.costing.material.entity.CommodityIndexValue;
import com.automotive.costing.material.entity.MaterialCostCalculation;
import com.automotive.costing.material.repository.MaterialCostCalculationRepository;
import com.automotive.costing.masterdata.entity.MasterDataStatus;
import com.automotive.costing.masterdata.entity.MaterialMaster;
import com.automotive.costing.masterdata.entity.ProcessMaster;
import com.automotive.costing.masterdata.repository.MaterialMasterRepository;
import com.automotive.costing.masterdata.repository.ProcessMasterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MaterialCostService, mapped to QA_Test_Case_Tracker.xlsx Module M03
 * (MAT-01, MAT-02, MAT-04, MAT-05, MAT-08) and the SRS 4.3.5 business rules governing
 * manual net-weight entry and scrap% override justification.
 */
@ExtendWith(MockitoExtension.class)
class MaterialCostServiceTest {

    @Mock private MaterialCostCalculationRepository calculationRepository;
    @Mock private MaterialMasterRepository materialMasterRepository;
    @Mock private TenantAccessGuard tenantAccessGuard;
    @Mock private ProcessMasterRepository processMasterRepository;
    @Mock private BomNodeRepository bomNodeRepository;
    @Mock private CommodityIndexService commodityIndexService;
    @Mock private AuditService auditService;
    @Mock private BomNodeService bomNodeService;

    private MaterialCostService service;

    @BeforeEach
    void setUp() {
        lenient().when(tenantAccessGuard.requireCustomerId()).thenReturn("cust-test-1");
        service = new MaterialCostService(calculationRepository, materialMasterRepository, tenantAccessGuard, processMasterRepository, bomNodeRepository, commodityIndexService, auditService, bomNodeService);
    }

    private BomNode bomNode() {
        BomNode node = new BomNode();
        node.setBomNodeId("node-1");
        node.setProgramId("program-1");
        node.setPartNumber("PN-001");
        return node;
    }

    private MaterialMaster fixedPriceMaterial() {
        MaterialMaster m = new MaterialMaster();
        m.setRecordId("mat-rec-1");
        m.setCode("MAT-STEEL-001");
        m.setRate(new BigDecimal("100.000000"));
        m.setStatus(MasterDataStatus.ACTIVE);
        m.setScrapRecoveryRate(new BigDecimal("20.000000"));
        m.setMaterialForm("sheet");
        m.setIndexLinkedFlag(false);
        return m;
    }

    private CalculateRequest cadPlmRequest(BigDecimal scrapOverride, String justification) {
        return new CalculateRequest("node-1", "MAT-STEEL-001", new BigDecimal("2.500000"),
                NetWeightSource.CAD_PLM, null, scrapOverride, null, justification, null);
    }

    // TC-0033 (MAT-01, Positive) + TC-0041 (MAT-05, Positive): a valid calculation is
    // persisted and immediately triggers a program roll-up.
    @Test
    void calculate_withValidCadPlmInput_savesAndTriggersProgramRollup() {
        when(bomNodeRepository.findByCustomerIdAndBomNodeId("cust-test-1", "node-1")).thenReturn(Optional.of(bomNode()));
        when(materialMasterRepository.findFirstByCustomerIdAndCodeOrderByVersionNumberDesc("cust-test-1", "MAT-STEEL-001"))
                .thenReturn(Optional.of(fixedPriceMaterial()));
        when(calculationRepository.save(any(MaterialCostCalculation.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Response result = service.calculate(cadPlmRequest(new BigDecimal("0.100000"), "supplier-confirmed 10% scrap"));

        assertThat(result.materialCost()).isEqualByComparingTo("272.222222");
        verify(bomNodeService).triggerRollupForProgram("program-1");
        verify(auditService).recordCreate(eq("MaterialCostCalculation"), anyString(), any());

        ArgumentCaptor<BomNode> savedNode = ArgumentCaptor.forClass(BomNode.class);
        verify(bomNodeRepository).save(savedNode.capture());
        assertThat(savedNode.getValue().getMaterialCode()).isEqualTo("MAT-STEEL-001");
    }

    // SRS 4.3.5: manual net-weight entry without supervisor approval must be rejected.
    @Test
    void calculate_withManualNetWeightAndNoApproval_throwsBusinessRuleViolation() {
        when(bomNodeRepository.findByCustomerIdAndBomNodeId("cust-test-1", "node-1")).thenReturn(Optional.of(bomNode()));
        CalculateRequest request = new CalculateRequest("node-1", "MAT-STEEL-001",
                new BigDecimal("2.500000"), NetWeightSource.MANUAL, null,
                new BigDecimal("0.1"), null, "justified", null);

        assertThatThrownBy(() -> service.calculate(request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("supervisor");

        verifyNoInteractions(calculationRepository);
    }

    // SRS 4.3.5: manual net weight WITH supervisor approval is accepted.
    @Test
    void calculate_withManualNetWeightAndApproval_succeeds() {
        when(bomNodeRepository.findByCustomerIdAndBomNodeId("cust-test-1", "node-1")).thenReturn(Optional.of(bomNode()));
        when(materialMasterRepository.findFirstByCustomerIdAndCodeOrderByVersionNumberDesc("cust-test-1", "MAT-STEEL-001"))
                .thenReturn(Optional.of(fixedPriceMaterial()));
        when(calculationRepository.save(any(MaterialCostCalculation.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        CalculateRequest request = new CalculateRequest("node-1", "MAT-STEEL-001",
                new BigDecimal("2.500000"), NetWeightSource.MANUAL, "supervisor.jones",
                new BigDecimal("0.100000"), null, "measured manually, approved", null);

        Response result = service.calculate(request);
        assertThat(result.materialCost()).isEqualByComparingTo("272.222222");
    }

    // SRS 4.3.5: a scrap% override without a justification is rejected.
    @Test
    void calculate_withScrapOverrideAndNoJustification_throwsBusinessRuleViolation() {
        when(bomNodeRepository.findByCustomerIdAndBomNodeId("cust-test-1", "node-1")).thenReturn(Optional.of(bomNode()));
        CalculateRequest request = cadPlmRequest(new BigDecimal("0.100000"), null);

        assertThatThrownBy(() -> service.calculate(request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("justification");

        verifyNoInteractions(calculationRepository);
    }

    // SRS 4.3.5: no override supplied -> defaults from the named process master.
    @Test
    void calculate_withoutScrapOverride_defaultsFromProcessMaster() {
        when(bomNodeRepository.findByCustomerIdAndBomNodeId("cust-test-1", "node-1")).thenReturn(Optional.of(bomNode()));
        when(materialMasterRepository.findFirstByCustomerIdAndCodeOrderByVersionNumberDesc("cust-test-1", "MAT-STEEL-001"))
                .thenReturn(Optional.of(fixedPriceMaterial()));
        when(calculationRepository.save(any(MaterialCostCalculation.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ProcessMaster stamping = new ProcessMaster();
        stamping.setProcessName("Stamping");
        stamping.setDefaultScrapPct(new BigDecimal("0.100000"));
        when(processMasterRepository.findByProcessName("Stamping")).thenReturn(Optional.of(stamping));

        CalculateRequest request = new CalculateRequest("node-1", "MAT-STEEL-001",
                new BigDecimal("2.500000"), NetWeightSource.CAD_PLM, null, null, "Stamping", null, null);

        Response result = service.calculate(request);
        assertThat(result.scrapPct()).isEqualByComparingTo("0.100000");
        assertThat(result.materialCost()).isEqualByComparingTo("272.222222");
    }

    // TC-0035 (MAT-02, Positive): index-linked material resolves rate from the commodity index.
    @Test
    void calculate_withIndexLinkedMaterial_resolvesRateFromCommodityIndex() {
        when(bomNodeRepository.findByCustomerIdAndBomNodeId("cust-test-1", "node-1")).thenReturn(Optional.of(bomNode()));

        MaterialMaster indexLinked = fixedPriceMaterial();
        indexLinked.setIndexLinkedFlag(true);
        indexLinked.setIndexCode("LME-AL");
        indexLinked.setBasePrice(new BigDecimal("1.200000"));
        indexLinked.setAlloySurchargePerUnit(new BigDecimal("0.075000"));
        when(materialMasterRepository.findFirstByCustomerIdAndCodeOrderByVersionNumberDesc("cust-test-1", "MAT-STEEL-001"))
                .thenReturn(Optional.of(indexLinked));

        CommodityIndexValue indexValue = new CommodityIndexValue();
        indexValue.setIndexValue(new BigDecimal("0.350000"));
        when(commodityIndexService.getEffectiveValue(eq("LME-AL"), any(LocalDate.class)))
                .thenReturn(indexValue);
        when(calculationRepository.save(any(MaterialCostCalculation.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Response result = service.calculate(cadPlmRequest(new BigDecimal("0.0"), "no scrap for this op"));

        assertThat(result.unitRate()).isEqualByComparingTo("1.625000");
    }

    @Test
    void calculate_forUnknownBomNode_throwsResourceNotFound() {
        when(bomNodeRepository.findByCustomerIdAndBomNodeId("cust-test-1", "missing")).thenReturn(Optional.empty());

        CalculateRequest request = new CalculateRequest("missing", "MAT-STEEL-001",
                new BigDecimal("2.500000"), NetWeightSource.CAD_PLM, null, null, null, null, null);

        assertThatThrownBy(() -> service.calculate(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // TC-0039/TC-0040 (MAT-04): comparison requires at least two material codes.
    @Test
    void compareAlternates_withFewerThanTwoMaterialCodes_throwsBusinessRuleViolation() {
        CompareRequest request = new CompareRequest(new BigDecimal("2.5"), new BigDecimal("0.1"),
                List.of("MAT-STEEL-001"));

        assertThatThrownBy(() -> service.compareAlternates(request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("MAT-04");
    }

    // TC-0047/TC-0048 (MAT-08): ratio above threshold is flagged, at/below is not.
    @Test
    void checkHighMaterialCostRatio_flagsWhenAboveThreshold() {
        HighMaterialCostCheckRequest above = new HighMaterialCostCheckRequest(
                new BigDecimal("60"), new BigDecimal("100"), new BigDecimal("0.50"));
        HighMaterialCostCheckResponse aboveResult = service.checkHighMaterialCostRatio(above);
        assertThat(aboveResult.flagged()).isTrue();

        HighMaterialCostCheckRequest atThreshold = new HighMaterialCostCheckRequest(
                new BigDecimal("50"), new BigDecimal("100"), new BigDecimal("0.50"));
        HighMaterialCostCheckResponse atResult = service.checkHighMaterialCostRatio(atThreshold);
        assertThat(atResult.flagged()).isFalse();
    }
}
