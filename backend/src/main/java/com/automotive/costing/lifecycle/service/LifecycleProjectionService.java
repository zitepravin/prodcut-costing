package com.automotive.costing.lifecycle.service;

import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.dashboard.dto.DashboardDtos.ProgramCostPositionResponse;
import com.automotive.costing.dashboard.service.ProgramCostPositionService;
import com.automotive.costing.lifecycle.dto.LifecycleDtos.LifecycleProjectionResponse;
import com.automotive.costing.lifecycle.dto.LifecycleDtos.YearProjectionResponse;
import com.automotive.costing.lifecycle.engine.LifecycleProjectionEngine;
import com.automotive.costing.lifecycle.entity.ProgramVolumeCurve;
import com.automotive.costing.lifecycle.entity.VolumeCurveStatus;
import com.automotive.costing.lifecycle.repository.ProgramVolumeCurveRepository;
import com.automotive.costing.lifecycle.repository.ProgramVolumeCurveYearRepository;
import com.automotive.costing.masterdata.entity.VehicleProgram;
import com.automotive.costing.masterdata.repository.VehicleProgramRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.platform.user.entity.ScopeType;
import com.automotive.costing.platform.user.service.AccessScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LifecycleProjectionService {

    private final AccessScopeService accessScopeService;
    private final ProgramVolumeCurveRepository curveRepository;
    private final ProgramVolumeCurveYearRepository yearRepository;
    private final ProgramCostPositionService programCostPositionService;
    private final VehicleProgramRepository vehicleProgramRepository;
    private final TenantAccessGuard tenantAccessGuard;

    @Transactional(readOnly = true)
    public LifecycleProjectionResponse project(String programId, String scenarioCode) {
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, programId);
        String code = scenarioCode == null || scenarioCode.isBlank() ? "BASE" : scenarioCode.trim().toUpperCase();

        ProgramVolumeCurve curve = curveRepository
                .findByProgramIdAndScenarioCodeAndStatus(programId, code, VolumeCurveStatus.ACTIVE)
                .or(() -> curveRepository.findFirstByProgramIdAndScenarioCodeOrderByCreatedAtDesc(programId, code))
                .orElseThrow(() -> ResourceNotFoundException.of("ProgramVolumeCurve", programId + "/" + code));

        var yearEntities = yearRepository.findByCurveIdOrderByYearIndexAsc(curve.getCurveId());
        List<LifecycleProjectionEngine.YearVolume> years = yearEntities.stream()
                .map(y -> new LifecycleProjectionEngine.YearVolume(
                        y.getYearIndex(), y.getCalendarYear(), y.getPlannedVolume()))
                .toList();

        ProgramCostPositionResponse position = programCostPositionService.position(
                programId, null, true);

        BigDecimal volumeInsensitive = null;
        BigDecimal toolingPerUnitBaseline = null;
        if (position.drivers() != null) {
            BigDecimal sum = BigDecimal.ZERO;
            boolean any = false;
            for (var d : position.drivers()) {
                if (d.amount() == null) {
                    continue;
                }
                if ("TOOLING".equalsIgnoreCase(d.componentCode())) {
                    toolingPerUnitBaseline = d.amount();
                } else {
                    sum = sum.add(d.amount());
                    any = true;
                }
            }
            if (any) {
                volumeInsensitive = sum;
            }
        }
        if (volumeInsensitive == null && position.shouldCostTotal() != null) {
            // fallback: total minus tooling if known
            volumeInsensitive = toolingPerUnitBaseline != null
                    ? position.shouldCostTotal().subtract(toolingPerUnitBaseline)
                    : position.shouldCostTotal();
        }

        String customerId = tenantAccessGuard.requireCustomerId();
        VehicleProgram program = vehicleProgramRepository.findByCustomerIdAndProgramId(customerId, programId).orElse(null);
        BigDecimal baselineLifetime = program != null ? program.getLifetimeVolume() : null;
        BigDecimal toolingCash = LifecycleProjectionEngine.toolingCash(toolingPerUnitBaseline, baselineLifetime);

        var result = LifecycleProjectionEngine.project(volumeInsensitive, toolingCash, years);

        List<String> notes = new ArrayList<>(result.dataQualityNotes());
        if (!"AVAILABLE".equals(position.shouldCostStatus()) && !"PARTIAL".equals(position.shouldCostStatus())) {
            notes.add(0, "Program should-cost position is " + position.shouldCostStatus() + ".");
        }

        List<YearProjectionResponse> yearOut = result.years().stream()
                .map(y -> new YearProjectionResponse(
                        y.yearIndex(),
                        y.calendarYear(),
                        y.plannedVolume(),
                        y.unitCost(),
                        y.yearTotalCost(),
                        y.cumulativeVolume(),
                        y.cumulativeCost()))
                .toList();

        String currency = position.reportingCurrency() != null ? position.reportingCurrency() : "INR";

        return new LifecycleProjectionResponse(
                programId,
                code,
                curve.getCurveId(),
                currency,
                result.unitCostStatus(),
                result.unitCostBaseline(),
                result.volumeInsensitiveUnit(),
                result.toolingPerUnit(),
                result.toolingStatus(),
                result.lifetimeVolumeForCurve(),
                yearOut,
                notes
        );
    }
}
