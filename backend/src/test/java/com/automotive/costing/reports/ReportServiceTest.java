package com.automotive.costing.reports;

import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.erp.entity.ErpBatchStatus;
import com.automotive.costing.erp.repository.ErpInboundBatchRepository;
import com.automotive.costing.masterdata.entity.MasterDataStatus;
import com.automotive.costing.masterdata.repository.LaborMasterRepository;
import com.automotive.costing.masterdata.repository.MachineMasterRepository;
import com.automotive.costing.masterdata.repository.MaterialMasterRepository;
import com.automotive.costing.masterdata.repository.PlantMasterRepository;
import com.automotive.costing.masterdata.repository.VehicleProgramRepository;
import com.automotive.costing.notification.repository.AppNotificationRepository;
import com.automotive.costing.packaging.repository.PackagingLogisticsProfileRepository;
import com.automotive.costing.reports.dto.ReportDtos.OperationalReport;
import com.automotive.costing.reports.dto.ReportDtos.ShouldCostElementReport;
import com.automotive.costing.reports.service.ReportService;
import com.automotive.costing.shouldcost.repository.ShouldCostBuildUpRepository;
import com.automotive.costing.targetcosting.entity.TargetCostStatus;
import com.automotive.costing.targetcosting.repository.TargetCostRecordRepository;
import com.automotive.costing.targetcosting.repository.TargetCostVarianceRepository;
import com.automotive.costing.workflow.entity.DecisionStatus;
import com.automotive.costing.workflow.repository.ApprovalStepInstanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ReportServiceTest {

    @Mock MaterialMasterRepository materialMasterRepository;
    @Mock private TenantAccessGuard tenantAccessGuard;
    @Mock MachineMasterRepository machineMasterRepository;
    @Mock LaborMasterRepository laborMasterRepository;
    @Mock PlantMasterRepository plantMasterRepository;
    @Mock VehicleProgramRepository vehicleProgramRepository;
    @Mock BomNodeRepository bomNodeRepository;
    @Mock PackagingLogisticsProfileRepository packagingLogisticsProfileRepository;
    @Mock ShouldCostBuildUpRepository shouldCostBuildUpRepository;
    @Mock TargetCostRecordRepository targetCostRecordRepository;
    @Mock TargetCostVarianceRepository targetCostVarianceRepository;
    @Mock ApprovalStepInstanceRepository approvalStepInstanceRepository;
    @Mock AppNotificationRepository appNotificationRepository;
    @Mock ErpInboundBatchRepository erpInboundBatchRepository;

    ReportService service;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(tenantAccessGuard.requireCustomerId()).thenReturn("cust-test-1");
        service = new ReportService(materialMasterRepository, machineMasterRepository, laborMasterRepository, plantMasterRepository, vehicleProgramRepository, bomNodeRepository, packagingLogisticsProfileRepository, shouldCostBuildUpRepository, tenantAccessGuard, targetCostRecordRepository, targetCostVarianceRepository, approvalStepInstanceRepository, appNotificationRepository, erpInboundBatchRepository);
    }

    @Test
    @DisplayName("Should-cost element totals use BigDecimal scale 6")
    void shouldCostElementsScaled() {
        when(shouldCostBuildUpRepository.countByCustomerId("cust-test-1")).thenReturn(2L);
        when(shouldCostBuildUpRepository.sumMaterialCostByCustomerId("cust-test-1")).thenReturn(new BigDecimal("10.5"));
        when(shouldCostBuildUpRepository.sumProcessCostByCustomerId("cust-test-1")).thenReturn(new BigDecimal("20"));
        when(shouldCostBuildUpRepository.sumToolingCostByCustomerId("cust-test-1")).thenReturn(BigDecimal.ZERO);
        when(shouldCostBuildUpRepository.sumPackagingFreightCostByCustomerId("cust-test-1")).thenReturn(BigDecimal.ZERO);
        when(shouldCostBuildUpRepository.sumOverheadCostByCustomerId("cust-test-1")).thenReturn(BigDecimal.ZERO);
        when(shouldCostBuildUpRepository.sumSgaCostByCustomerId("cust-test-1")).thenReturn(BigDecimal.ZERO);
        when(shouldCostBuildUpRepository.sumProfitMarginByCustomerId("cust-test-1")).thenReturn(BigDecimal.ZERO);
        when(shouldCostBuildUpRepository.sumTotalCostByCustomerId("cust-test-1")).thenReturn(new BigDecimal("30.5"));

        ShouldCostElementReport r = service.shouldCostElements(null);
        assertEquals(2L, r.buildUpCount());
        assertEquals(new BigDecimal("10.500000"), r.materialCostTotal());
        assertEquals(new BigDecimal("30.500000"), r.grandTotal());
    }

    @Test
    @DisplayName("Program-scoped should-cost uses program repository methods")
    void programScopedShouldCost() {
        when(vehicleProgramRepository.existsById("prog-1")).thenReturn(true);
        when(shouldCostBuildUpRepository.countByProgramId("prog-1")).thenReturn(1L);
        when(shouldCostBuildUpRepository.sumMaterialCostByProgramId("prog-1")).thenReturn(new BigDecimal("5"));
        when(shouldCostBuildUpRepository.sumProcessCostByProgramId("prog-1")).thenReturn(BigDecimal.ZERO);
        when(shouldCostBuildUpRepository.sumToolingCostByProgramId("prog-1")).thenReturn(BigDecimal.ZERO);
        when(shouldCostBuildUpRepository.sumPackagingFreightCostByProgramId("prog-1")).thenReturn(BigDecimal.ZERO);
        when(shouldCostBuildUpRepository.sumOverheadCostByProgramId("prog-1")).thenReturn(BigDecimal.ZERO);
        when(shouldCostBuildUpRepository.sumSgaCostByProgramId("prog-1")).thenReturn(BigDecimal.ZERO);
        when(shouldCostBuildUpRepository.sumProfitMarginByProgramId("prog-1")).thenReturn(BigDecimal.ZERO);
        when(shouldCostBuildUpRepository.sumTotalCostByProgramId("prog-1")).thenReturn(new BigDecimal("5"));

        ShouldCostElementReport r = service.shouldCostElements("prog-1");
        assertEquals(1L, r.buildUpCount());
        assertEquals(new BigDecimal("5.000000"), r.materialCostTotal());
    }

    @Test
    @DisplayName("CSV export contains section headers")
    void csvExport() {
        stubSystemWide();
        String csv = service.operationalCsv(null);
        assertTrue(csv.startsWith("section,metric,value"));
        assertTrue(csv.contains("shouldCost,grandTotal,"));
        assertTrue(csv.contains("scope,programId,ALL"));
    }

    private void stubSystemWide() {
        when(materialMasterRepository.count()).thenReturn(5L);
        when(materialMasterRepository.countByStatus(MasterDataStatus.ACTIVE)).thenReturn(3L);
        when(machineMasterRepository.count()).thenReturn(2L);
        when(machineMasterRepository.countByStatus(MasterDataStatus.ACTIVE)).thenReturn(2L);
        when(laborMasterRepository.count()).thenReturn(4L);
        when(laborMasterRepository.countByStatus(MasterDataStatus.ACTIVE)).thenReturn(4L);
        when(plantMasterRepository.count()).thenReturn(1L);
        when(vehicleProgramRepository.count()).thenReturn(1L);
        when(erpInboundBatchRepository.count()).thenReturn(0L);
        when(erpInboundBatchRepository.countGroupedByStatus()).thenReturn(List.of());
        when(shouldCostBuildUpRepository.count()).thenReturn(0L);
        when(shouldCostBuildUpRepository.sumMaterialCostByCustomerId("cust-test-1")).thenReturn(null);
        when(shouldCostBuildUpRepository.sumProcessCostByCustomerId("cust-test-1")).thenReturn(null);
        when(shouldCostBuildUpRepository.sumToolingCostByCustomerId("cust-test-1")).thenReturn(null);
        when(shouldCostBuildUpRepository.sumPackagingFreightCostByCustomerId("cust-test-1")).thenReturn(null);
        when(shouldCostBuildUpRepository.sumOverheadCostByCustomerId("cust-test-1")).thenReturn(null);
        when(shouldCostBuildUpRepository.sumSgaCostByCustomerId("cust-test-1")).thenReturn(null);
        when(shouldCostBuildUpRepository.sumProfitMarginByCustomerId("cust-test-1")).thenReturn(null);
        when(shouldCostBuildUpRepository.sumTotalCostByCustomerId("cust-test-1")).thenReturn(null);
        when(targetCostVarianceRepository.count()).thenReturn(0L);
        when(targetCostVarianceRepository.countByExceedsThresholdTrue()).thenReturn(0L);
        when(targetCostVarianceRepository.countByExceedsThresholdFalse()).thenReturn(0L);
        when(targetCostVarianceRepository.sumAbsoluteVariance()).thenReturn(BigDecimal.ZERO);
        when(targetCostVarianceRepository.avgPercentageVariance()).thenReturn(BigDecimal.ZERO);
        when(bomNodeRepository.count()).thenReturn(10L);
        when(packagingLogisticsProfileRepository.count()).thenReturn(0L);
        when(targetCostRecordRepository.countByStatus(TargetCostStatus.ACTIVE)).thenReturn(1L);
        when(approvalStepInstanceRepository.countByDecision(DecisionStatus.PENDING)).thenReturn(0L);
        when(appNotificationRepository.count()).thenReturn(0L);
    }
}
