package com.automotive.costing.dashboard.service;

import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.dashboard.dto.DashboardDtos.CoverageDto;
import com.automotive.costing.dashboard.dto.DashboardDtos.DriverAmountDto;
import com.automotive.costing.dashboard.dto.DashboardDtos.PositionAmountStatus;
import com.automotive.costing.dashboard.dto.DashboardDtos.ProgramCostPositionResponse;
import com.automotive.costing.dashboard.engine.ProgramCostPositionEngine;
import com.automotive.costing.masterdata.entity.VehicleProgram;
import com.automotive.costing.masterdata.repository.VehicleProgramRepository;
import com.automotive.costing.platform.user.entity.ScopeType;
import com.automotive.costing.platform.user.service.AccessScopeService;
import com.automotive.costing.shouldcost.entity.ShouldCostBuildUp;
import com.automotive.costing.shouldcost.repository.ShouldCostBuildUpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Program cost position — backend-authoritative totals for management dashboard.
 * Does not use unsafe sum*ByProgramId (those double-count SC-06 scenarios).
 */
@Service
@RequiredArgsConstructor
public class ProgramCostPositionService {

    private final VehicleProgramRepository vehicleProgramRepository;
    private final ShouldCostBuildUpRepository shouldCostBuildUpRepository;
    private final BomNodeRepository bomNodeRepository;
    private final AccessScopeService accessScopeService;

    @Transactional(readOnly = true)
    public ProgramCostPositionResponse position(String programId, String scenarioPolicy, boolean includeDrivers) {
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, programId);

        VehicleProgram program = vehicleProgramRepository.findById(programId)
                .orElseThrow(() -> ResourceNotFoundException.of("VehicleProgram", programId));

        String policy = (scenarioPolicy == null || scenarioPolicy.isBlank())
                ? ProgramCostPositionEngine.POLICY_DEFAULT
                : scenarioPolicy.trim();

        List<ShouldCostBuildUp> candidates = shouldCostBuildUpRepository.findByProgramId(programId);
        List<ShouldCostBuildUp> selected = ProgramCostPositionEngine.selectOnePerBomNode(candidates, policy);

        long bomNodesInProgram = bomNodeRepository.countByProgramId(programId);
        long nodesWithSelected = selected.size();
        long missing = Math.max(0, bomNodesInProgram - nodesWithSelected);
        long considered = candidates.size();

        BigDecimal coverageRatio = BigDecimal.ZERO;
        if (bomNodesInProgram > 0) {
            coverageRatio = BigDecimal.valueOf(nodesWithSelected)
                    .divide(BigDecimal.valueOf(bomNodesInProgram), ProgramCostPositionEngine.SCALE, RoundingMode.HALF_UP);
        }

        List<String> notes = new ArrayList<>();
        String shouldStatus;
        BigDecimal shouldTotal = null;
        ProgramCostPositionEngine.DriverSum driversSum = null;

        if (selected.isEmpty()) {
            shouldStatus = PositionAmountStatus.NOT_AVAILABLE.name();
            notes.add("No should-cost build-up selected under policy " + policy + ".");
        } else {
            driversSum = ProgramCostPositionEngine.sumDrivers(selected);
            shouldTotal = driversSum.total();
            shouldStatus = missing > 0
                    ? PositionAmountStatus.PARTIAL.name()
                    : PositionAmountStatus.AVAILABLE.name();
            if (missing > 0) {
                notes.add(missing + " BOM node(s) have no should-cost under selection policy.");
            }
        }

        // Target: VehicleProgram.targetCost first
        BigDecimal targetTotal = null;
        String targetStatus;
        String targetSource = null;
        if (program.getTargetCost() != null) {
            targetTotal = program.getTargetCost().setScale(ProgramCostPositionEngine.SCALE, RoundingMode.HALF_UP);
            targetStatus = PositionAmountStatus.AVAILABLE.name();
            targetSource = "VEHICLE_PROGRAM";
        } else {
            targetStatus = PositionAmountStatus.NOT_AVAILABLE.name();
            notes.add("VehicleProgram.targetCost is not set; program target is NOT AVAILABLE.");
        }

        String reportingCurrency = program.getTargetCurrency() != null && !program.getTargetCurrency().isBlank()
                ? program.getTargetCurrency()
                : "INR";

        String varianceStatus;
        BigDecimal absVar = null;
        BigDecimal pctVar = null;
        if (shouldTotal != null && targetTotal != null) {
            var v = ProgramCostPositionEngine.variance(shouldTotal, targetTotal);
            if (v != null) {
                absVar = v.absoluteVariance();
                pctVar = v.percentageVariance();
                varianceStatus = PositionAmountStatus.AVAILABLE.name();
            } else {
                varianceStatus = PositionAmountStatus.NOT_AVAILABLE.name();
            }
        } else {
            varianceStatus = PositionAmountStatus.NOT_AVAILABLE.name();
        }

        List<DriverAmountDto> drivers = List.of();
        if (includeDrivers && driversSum != null) {
            drivers = List.of(
                    driver("MATERIAL", "Material", driversSum.material()),
                    driver("PROCESS", "Process", driversSum.process()),
                    driver("TOOLING", "Tooling", driversSum.tooling()),
                    driver("PACKAGING_FREIGHT", "Packaging & Freight", driversSum.packagingFreight()),
                    driver("OVERHEAD", "Overhead", driversSum.overhead()),
                    driver("SGA", "SG&A", driversSum.sga()),
                    driver("PROFIT_MARGIN", "Profit Margin", driversSum.profitMargin())
            );
        }

        CoverageDto coverage = new CoverageDto(
                bomNodesInProgram,
                nodesWithSelected,
                missing,
                0L,
                considered,
                coverageRatio
        );

        return new ProgramCostPositionResponse(
                program.getProgramId(),
                program.getProgramCode(),
                program.getModelName(),
                reportingCurrency,
                Instant.now(),
                policy,
                shouldTotal,
                shouldStatus,
                targetTotal,
                targetStatus,
                targetSource,
                absVar,
                pctVar,
                varianceStatus,
                coverage,
                drivers,
                notes
        );
    }

    private static DriverAmountDto driver(String code, String name, BigDecimal amount) {
        return new DriverAmountDto(code, name, amount, PositionAmountStatus.AVAILABLE.name());
    }
}
