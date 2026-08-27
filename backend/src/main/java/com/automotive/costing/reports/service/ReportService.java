package com.automotive.costing.reports.service;

import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.common.exception.ResourceNotFoundException;
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
import com.automotive.costing.reports.dto.ReportDtos.ErpStagingReport;
import com.automotive.costing.reports.dto.ReportDtos.MasterDataHealthReport;
import com.automotive.costing.reports.dto.ReportDtos.OperationalReport;
import com.automotive.costing.reports.dto.ReportDtos.ShouldCostElementReport;
import com.automotive.costing.reports.dto.ReportDtos.TargetVarianceReport;
import com.automotive.costing.shouldcost.repository.ShouldCostBuildUpRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.targetcosting.entity.TargetCostStatus;
import com.automotive.costing.targetcosting.repository.TargetCostRecordRepository;
import com.automotive.costing.targetcosting.repository.TargetCostVarianceRepository;
import com.automotive.costing.workflow.entity.DecisionStatus;
import com.automotive.costing.workflow.repository.ApprovalStepInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Operational and cost analytics reports. Read-only aggregates;
 * monetary values use BigDecimal with scale 6.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private static final int MONEY_SCALE = 6;

    private final MaterialMasterRepository materialMasterRepository;
    private final MachineMasterRepository machineMasterRepository;
    private final LaborMasterRepository laborMasterRepository;
    private final PlantMasterRepository plantMasterRepository;
    private final VehicleProgramRepository vehicleProgramRepository;
    private final BomNodeRepository bomNodeRepository;
    private final PackagingLogisticsProfileRepository packagingLogisticsProfileRepository;
    private final ShouldCostBuildUpRepository shouldCostBuildUpRepository;
    private final TenantAccessGuard tenantAccessGuard;
    private final TargetCostRecordRepository targetCostRecordRepository;
    private final TargetCostVarianceRepository targetCostVarianceRepository;
    private final ApprovalStepInstanceRepository approvalStepInstanceRepository;
    private final AppNotificationRepository appNotificationRepository;
    private final ErpInboundBatchRepository erpInboundBatchRepository;

    @Transactional(readOnly = true)
    public MasterDataHealthReport masterDataHealth() {
        return new MasterDataHealthReport(
                materialMasterRepository.count(),
                materialMasterRepository.countByStatus(MasterDataStatus.ACTIVE),
                machineMasterRepository.count(),
                machineMasterRepository.countByStatus(MasterDataStatus.ACTIVE),
                laborMasterRepository.count(),
                laborMasterRepository.countByStatus(MasterDataStatus.ACTIVE),
                plantMasterRepository.count(),
                vehicleProgramRepository.count()
        );
    }

    @Transactional(readOnly = true)
    public ErpStagingReport erpStaging() {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (ErpBatchStatus s : ErpBatchStatus.values()) {
            byStatus.put(s.name(), 0L);
        }
        for (Object[] row : erpInboundBatchRepository.countGroupedByStatus()) {
            if (row == null || row[0] == null) {
                continue;
            }
            String statusName = row[0] instanceof ErpBatchStatus e ? e.name() : String.valueOf(row[0]);
            long count = row[1] instanceof Number n ? n.longValue() : 0L;
            byStatus.put(statusName, count);
        }
        return new ErpStagingReport(erpInboundBatchRepository.count(), byStatus);
    }

    @Transactional(readOnly = true)
    public ShouldCostElementReport shouldCostElements(String programId) {
        if (programId == null || programId.isBlank()) {
            return new ShouldCostElementReport(
                    shouldCostBuildUpRepository.countByCustomerId(tenantAccessGuard.requireCustomerId()),
                    scale(shouldCostBuildUpRepository.sumMaterialCostByCustomerId(tenantAccessGuard.requireCustomerId())),
                    scale(shouldCostBuildUpRepository.sumProcessCostByCustomerId(tenantAccessGuard.requireCustomerId())),
                    scale(shouldCostBuildUpRepository.sumToolingCostByCustomerId(tenantAccessGuard.requireCustomerId())),
                    scale(shouldCostBuildUpRepository.sumPackagingFreightCostByCustomerId(tenantAccessGuard.requireCustomerId())),
                    scale(shouldCostBuildUpRepository.sumOverheadCostByCustomerId(tenantAccessGuard.requireCustomerId())),
                    scale(shouldCostBuildUpRepository.sumSgaCostByCustomerId(tenantAccessGuard.requireCustomerId())),
                    scale(shouldCostBuildUpRepository.sumProfitMarginByCustomerId(tenantAccessGuard.requireCustomerId())),
                    scale(shouldCostBuildUpRepository.sumTotalCostByCustomerId(tenantAccessGuard.requireCustomerId()))
            );
        }
        assertProgramExists(programId);
        return new ShouldCostElementReport(
                shouldCostBuildUpRepository.countByProgramId(programId),
                scale(shouldCostBuildUpRepository.sumMaterialCostByProgramId(programId)),
                scale(shouldCostBuildUpRepository.sumProcessCostByProgramId(programId)),
                scale(shouldCostBuildUpRepository.sumToolingCostByProgramId(programId)),
                scale(shouldCostBuildUpRepository.sumPackagingFreightCostByProgramId(programId)),
                scale(shouldCostBuildUpRepository.sumOverheadCostByProgramId(programId)),
                scale(shouldCostBuildUpRepository.sumSgaCostByProgramId(programId)),
                scale(shouldCostBuildUpRepository.sumProfitMarginByProgramId(programId)),
                scale(shouldCostBuildUpRepository.sumTotalCostByProgramId(programId))
        );
    }

    @Transactional(readOnly = true)
    public TargetVarianceReport targetVariance(String programId) {
        if (programId == null || programId.isBlank()) {
            return new TargetVarianceReport(
                    targetCostVarianceRepository.count(),
                    targetCostVarianceRepository.countByExceedsThresholdTrue(),
                    targetCostVarianceRepository.countByExceedsThresholdFalse(),
                    scale(targetCostVarianceRepository.sumAbsoluteVariance()),
                    scale(targetCostVarianceRepository.avgPercentageVariance())
            );
        }
        assertProgramExists(programId);
        return new TargetVarianceReport(
                targetCostVarianceRepository.countByProgramId(programId),
                targetCostVarianceRepository.countByProgramIdAndExceedsThresholdTrue(programId),
                targetCostVarianceRepository.countByProgramIdAndExceedsThresholdFalse(programId),
                scale(targetCostVarianceRepository.sumAbsoluteVarianceByProgramId(programId)),
                scale(targetCostVarianceRepository.avgPercentageVarianceByProgramId(programId))
        );
    }

    @Transactional(readOnly = true)
    public OperationalReport operational(String programId) {
        String scope = (programId == null || programId.isBlank()) ? null : programId.trim();
        if (scope != null) {
            assertProgramExists(scope);
        }

        long bomCount = safeLong(() -> scope == null
                ? bomNodeRepository.count()
                : bomNodeRepository.countByProgramId(scope), "bomNodeCount");
        long activeTargets = safeLong(() -> scope == null
                ? targetCostRecordRepository.countByStatus(TargetCostStatus.ACTIVE)
                : targetCostRecordRepository.countByProgramIdAndStatus(scope, TargetCostStatus.ACTIVE), "activeTargets");
        long packagingCount = safeLong(packagingLogisticsProfileRepository::count, "packagingProfiles");
        long pendingApprovals = safeLong(
                () -> approvalStepInstanceRepository.countByDecision(DecisionStatus.PENDING), "pendingApprovals");
        long notifications = safeLong(appNotificationRepository::count, "notifications");

        MasterDataHealthReport masterData;
        try {
            masterData = masterDataHealth();
        } catch (Exception e) {
            log.warn("masterDataHealth failed: {}", e.toString());
            masterData = new MasterDataHealthReport(0, 0, 0, 0, 0, 0, 0, 0);
        }

        ErpStagingReport erp;
        try {
            erp = erpStaging();
        } catch (Exception e) {
            log.warn("erpStaging report failed: {}", e.toString());
            erp = new ErpStagingReport(0, Map.of());
        }

        ShouldCostElementReport shouldCost;
        try {
            shouldCost = shouldCostElements(scope);
        } catch (Exception e) {
            log.warn("shouldCostElements failed for program={}: {}", scope, e.toString());
            shouldCost = new ShouldCostElementReport(
                    0L,
                    BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        }

        TargetVarianceReport variance;
        try {
            variance = targetVariance(scope);
        } catch (Exception e) {
            log.warn("targetVariance failed for program={}: {}", scope, e.toString());
            variance = new TargetVarianceReport(
                    0L, 0L, 0L,
                    BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        }

        return new OperationalReport(
                scope,
                masterData,
                erp,
                shouldCost,
                variance,
                bomCount,
                packagingCount,
                activeTargets,
                pendingApprovals,
                notifications
        );
    }

    private static long safeLong(java.util.concurrent.Callable<Long> call, String label) {
        try {
            Long v = call.call();
            return v != null ? v : 0L;
        } catch (Exception e) {
            log.warn("Report metric {} failed: {}", label, e.toString());
            return 0L;
        }
    }

    /**
     * CSV export of the operational report (sectioned tables). UTF-8 text.
     */
    @Transactional(readOnly = true)
    public String operationalCsv(String programId) {
        OperationalReport r = operational(programId);
        StringBuilder sb = new StringBuilder();
        sb.append("section,metric,value\n");
        sb.append("scope,programId,").append(csv(r.programId() == null ? "ALL" : r.programId())).append('\n');
        sb.append("summary,bomNodeCount,").append(r.bomNodeCount()).append('\n');
        sb.append("summary,packagingProfileCount,").append(r.packagingProfileCount()).append('\n');
        sb.append("summary,activeTargetCosts,").append(r.activeTargetCosts()).append('\n');
        sb.append("summary,pendingApprovalSteps,").append(r.pendingApprovalSteps()).append('\n');
        sb.append("summary,notificationCount,").append(r.notificationCount()).append('\n');

        var md = r.masterData();
        sb.append("masterData,materialTotal,").append(md.materialTotal()).append('\n');
        sb.append("masterData,materialActive,").append(md.materialActive()).append('\n');
        sb.append("masterData,machineTotal,").append(md.machineTotal()).append('\n');
        sb.append("masterData,machineActive,").append(md.machineActive()).append('\n');
        sb.append("masterData,laborTotal,").append(md.laborTotal()).append('\n');
        sb.append("masterData,laborActive,").append(md.laborActive()).append('\n');
        sb.append("masterData,plantTotal,").append(md.plantTotal()).append('\n');
        sb.append("masterData,programTotal,").append(md.programTotal()).append('\n');

        var erp = r.erpStaging();
        sb.append("erpStaging,totalBatches,").append(erp.totalBatches()).append('\n');
        for (Map.Entry<String, Long> e : erp.byStatus().entrySet()) {
            sb.append("erpStaging,status.").append(e.getKey()).append(',').append(e.getValue()).append('\n');
        }

        var sc = r.shouldCostElements();
        sb.append("shouldCost,buildUpCount,").append(sc.buildUpCount()).append('\n');
        sb.append("shouldCost,materialCostTotal,").append(sc.materialCostTotal()).append('\n');
        sb.append("shouldCost,processCostTotal,").append(sc.processCostTotal()).append('\n');
        sb.append("shouldCost,toolingCostTotal,").append(sc.toolingCostTotal()).append('\n');
        sb.append("shouldCost,packagingFreightCostTotal,").append(sc.packagingFreightCostTotal()).append('\n');
        sb.append("shouldCost,overheadCostTotal,").append(sc.overheadCostTotal()).append('\n');
        sb.append("shouldCost,sgaCostTotal,").append(sc.sgaCostTotal()).append('\n');
        sb.append("shouldCost,profitMarginTotal,").append(sc.profitMarginTotal()).append('\n');
        sb.append("shouldCost,grandTotal,").append(sc.grandTotal()).append('\n');

        var tv = r.targetVariance();
        sb.append("targetVariance,varianceSnapshots,").append(tv.varianceSnapshots()).append('\n');
        sb.append("targetVariance,exceedingThreshold,").append(tv.exceedingThreshold()).append('\n');
        sb.append("targetVariance,withinThreshold,").append(tv.withinThreshold()).append('\n');
        sb.append("targetVariance,sumAbsoluteVariance,").append(tv.sumAbsoluteVariance()).append('\n');
        sb.append("targetVariance,avgPercentageVariance,").append(tv.avgPercentageVariance()).append('\n');

        return sb.toString();
    }

    private void assertProgramExists(String programId) {
        if (!vehicleProgramRepository.existsById(programId)) {
            throw ResourceNotFoundException.of("VehicleProgram", programId);
        }
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static BigDecimal scale(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        }
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
