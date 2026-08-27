package com.automotive.costing.emissions.service;

import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.emissions.dto.EmissionsDtos.EmissionsSnapshotResponse;
import com.automotive.costing.emissions.entity.EmissionFactor;
import com.automotive.costing.emissions.entity.ShouldCostEmissionsSnapshot;
import com.automotive.costing.emissions.repository.EmissionFactorRepository;
import com.automotive.costing.emissions.repository.ShouldCostEmissionsSnapshotRepository;
import com.automotive.costing.masterdata.entity.MaterialMaster;
import com.automotive.costing.masterdata.repository.MaterialMasterRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.material.entity.MaterialCostCalculation;
import com.automotive.costing.material.repository.MaterialCostCalculationRepository;
import com.automotive.costing.process.entity.ProcessOperation;
import com.automotive.costing.process.entity.ProcessRouting;
import com.automotive.costing.process.repository.ProcessOperationRepository;
import com.automotive.costing.process.repository.ProcessRoutingRepository;
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
import java.util.Optional;
import java.util.UUID;

/**
 * Material + process CO₂e on should-cost (market-gap process carbon).
 * Missing factor/qty → NOT_AVAILABLE (never zero-fill invent).
 * Does not change money should-cost totals.
 */
@Service
@RequiredArgsConstructor
public class ShouldCostEmissionsService {

    public static final String NOT_AVAILABLE = "NOT_AVAILABLE";
    public static final String AVAILABLE = "AVAILABLE";
    public static final String PARTIAL = "PARTIAL";
    private static final int SCALE = 6;
    private static final BigDecimal SECONDS_PER_HOUR = new BigDecimal("3600");

    private final ShouldCostEmissionsSnapshotRepository snapshotRepository;
    private final ShouldCostBuildUpRepository buildUpRepository;
    private final MaterialCostCalculationRepository materialCostCalculationRepository;
    private final MaterialMasterRepository materialMasterRepository;
    private final TenantAccessGuard tenantAccessGuard;
    private final EmissionFactorRepository factorRepository;
    private final ProcessRoutingRepository processRoutingRepository;
    private final ProcessOperationRepository processOperationRepository;

    @Transactional(readOnly = true)
    public EmissionsSnapshotResponse getForBuildUp(String buildUpId) {
        return snapshotRepository.findByBuildUpId(buildUpId)
                .map(s -> toResponse(s, List.of()))
                .orElseThrow(() -> new ResourceNotFoundException("Emissions snapshot", buildUpId));
    }

    @Transactional
    public EmissionsSnapshotResponse computeForBuildUp(String buildUpId) {
        String customerId = tenantAccessGuard.requireCustomerId();
        ShouldCostBuildUp bu = buildUpRepository.findByCustomerIdAndBuildUpId(customerId, buildUpId)
                .orElseThrow(() -> new ResourceNotFoundException("Should-cost build-up", buildUpId));

        List<String> notes = new ArrayList<>();
        notes.add("Market-gap emissions: material + process when data exists.");
        notes.add("Money should-cost totals are unchanged.");
        notes.add("Indicative CO2e only — not a certified LCA.");

        BigDecimal materialCo2e = null;
        BigDecimal processCo2e = null;
        String factorId = null;
        String factorCode = null;
        Integer factorVersion = null;
        BigDecimal qtyKg = null;

        // --- Material ---
        Optional<MaterialCostCalculation> mcOpt = materialCostCalculationRepository
                .findFirstByBomNodeIdOrderByCalculatedAtDesc(bu.getBomNodeId());
        if (mcOpt.isEmpty()) {
            notes.add("No material cost calculation — material CO2e DATA_NOT_AVAILABLE.");
        } else {
            MaterialCostCalculation mc = mcOpt.get();
            qtyKg = mc.getGrossWeight() != null ? mc.getGrossWeight()
                    : (mc.getNetWeight() != null ? mc.getNetWeight() : null);
            if (qtyKg == null || qtyKg.compareTo(BigDecimal.ZERO) <= 0) {
                notes.add("Material weight missing — material CO2e DATA_NOT_AVAILABLE.");
            } else {
                Optional<MaterialMaster> material = mc.getMaterialCode() == null
                        ? Optional.empty()
                        : materialMasterRepository.findFirstByCustomerIdAndCodeOrderByVersionNumberDesc(tenantAccessGuard.requireCustomerId(), mc.getMaterialCode());
                if (material.isEmpty() || material.get().getDefaultEmissionFactorId() == null
                        || material.get().getDefaultEmissionFactorId().isBlank()) {
                    notes.add("Material has no default emission factor — material CO2e DATA_NOT_AVAILABLE.");
                } else {
                    Optional<EmissionFactor> factorOpt = factorRepository.findById(material.get().getDefaultEmissionFactorId());
                    if (factorOpt.isEmpty() || !"ACTIVE".equals(factorOpt.get().getStatus())) {
                        notes.add("Linked emission factor missing or not ACTIVE — material CO2e DATA_NOT_AVAILABLE.");
                    } else {
                        EmissionFactor factor = factorOpt.get();
                        if (!"KG_CO2E_PER_KG".equalsIgnoreCase(factor.getUnit())) {
                            notes.add("Factor unit mismatch (expected KG_CO2E_PER_KG) — material CO2e DATA_NOT_AVAILABLE.");
                        } else {
                            materialCo2e = qtyKg.multiply(factor.getCo2eValue()).setScale(SCALE, RoundingMode.HALF_UP);
                            factorId = factor.getFactorId();
                            factorCode = factor.getFactorCode();
                            factorVersion = factor.getVersionNo();
                            notes.add("material_co2e_kg = weight × factor (" + factorCode + " v" + factorVersion + ").");
                        }
                    }
                }
            }
        }

        // --- Process ---
        Optional<EmissionFactor> processFactor = factorRepository
                .findFirstByCategoryAndStatusOrderByVersionNoDesc("PROCESS", "ACTIVE");
        BigDecimal processHours = totalProcessHours(bu.getBomNodeId());
        if (processHours == null) {
            notes.add("No process routing cycle times — process CO2e DATA_NOT_AVAILABLE.");
        } else if (processFactor.isEmpty()) {
            notes.add("No ACTIVE PROCESS emission factor (KG_CO2E_PER_HOUR) — process CO2e DATA_NOT_AVAILABLE.");
        } else {
            EmissionFactor pf = processFactor.get();
            if (!"KG_CO2E_PER_HOUR".equalsIgnoreCase(pf.getUnit())) {
                notes.add("PROCESS factor unit must be KG_CO2E_PER_HOUR — process CO2e DATA_NOT_AVAILABLE.");
            } else {
                processCo2e = processHours.multiply(pf.getCo2eValue()).setScale(SCALE, RoundingMode.HALF_UP);
                notes.add("process_co2e_kg = process_hours(" + processHours.toPlainString()
                        + ") × factor (" + pf.getFactorCode() + ").");
                if (factorCode == null) {
                    factorId = pf.getFactorId();
                    factorCode = pf.getFactorCode();
                    factorVersion = pf.getVersionNo();
                }
            }
        }

        BigDecimal total = null;
        String status;
        if (materialCo2e == null && processCo2e == null) {
            status = NOT_AVAILABLE;
        } else if (materialCo2e == null || processCo2e == null) {
            status = PARTIAL;
            total = (materialCo2e != null ? materialCo2e : BigDecimal.ZERO)
                    .add(processCo2e != null ? processCo2e : BigDecimal.ZERO)
                    .setScale(SCALE, RoundingMode.HALF_UP);
            // do not present missing as zero in notes
            notes.add("Status PARTIAL: missing component remains DATA_NOT_AVAILABLE in notes; total sums available parts only.");
        } else {
            status = AVAILABLE;
            total = materialCo2e.add(processCo2e).setScale(SCALE, RoundingMode.HALF_UP);
        }

        return persist(bu, materialCo2e, processCo2e, total, status, factorId, factorCode, factorVersion, qtyKg, notes);
    }

    private BigDecimal totalProcessHours(String bomNodeId) {
        List<ProcessRouting> routings = processRoutingRepository.findByBomNodeId(bomNodeId);
        if (routings.isEmpty()) return null;
        BigDecimal seconds = BigDecimal.ZERO;
        boolean any = false;
        for (ProcessRouting r : routings) {
            List<ProcessOperation> ops = processOperationRepository.findByRoutingIdOrderBySequenceNumberAsc(r.getRoutingId());
            for (ProcessOperation op : ops) {
                if (op.getCycleTimeSeconds() != null && op.getCycleTimeSeconds().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal parts = op.getOutputPartsPerCycle() != null
                            && op.getOutputPartsPerCycle().compareTo(BigDecimal.ZERO) > 0
                            ? op.getOutputPartsPerCycle() : BigDecimal.ONE;
                    // hours per part = cycle_seconds / parts_per_cycle / 3600
                    BigDecimal hrs = op.getCycleTimeSeconds()
                            .divide(parts, 10, RoundingMode.HALF_UP)
                            .divide(SECONDS_PER_HOUR, 10, RoundingMode.HALF_UP);
                    seconds = seconds.add(hrs);
                    any = true;
                }
            }
        }
        return any ? seconds.setScale(SCALE, RoundingMode.HALF_UP) : null;
    }

    private EmissionsSnapshotResponse persist(
            ShouldCostBuildUp bu,
            BigDecimal materialCo2e,
            BigDecimal processCo2e,
            BigDecimal totalCo2e,
            String status,
            String factorId,
            String factorCode,
            Integer factorVersion,
            BigDecimal qtyKg,
            List<String> notes) {

        ShouldCostEmissionsSnapshot s = snapshotRepository.findByBuildUpId(bu.getBuildUpId())
                .orElseGet(ShouldCostEmissionsSnapshot::new);
        if (s.getSnapshotId() == null) {
            s.setSnapshotId(UUID.randomUUID().toString());
        }
        s.setBuildUpId(bu.getBuildUpId());
        s.setBomNodeId(bu.getBomNodeId());
        s.setMaterialCo2eKg(materialCo2e);
        s.setProcessCo2eKg(processCo2e);
        s.setTotalCo2eKg(totalCo2e);
        s.setEmissionsStatus(status);
        s.setFactorId(factorId);
        s.setFactorCode(factorCode);
        s.setFactorVersionNo(factorVersion);
        s.setQuantityKg(qtyKg);
        s.setCalculationNotes(String.join(" ", notes));
        s.setCalculatedAt(Instant.now());
        snapshotRepository.save(s);
        return toResponse(s, notes);
    }

    private EmissionsSnapshotResponse toResponse(ShouldCostEmissionsSnapshot s, List<String> notes) {
        return new EmissionsSnapshotResponse(
                s.getSnapshotId(), s.getBuildUpId(), s.getBomNodeId(),
                s.getMaterialCo2eKg(), s.getProcessCo2eKg(), s.getTotalCo2eKg(),
                s.getEmissionsStatus(), s.getFactorId(), s.getFactorCode(), s.getFactorVersionNo(),
                s.getQuantityKg(), s.getCalculationNotes(), s.getCalculatedAt(),
                notes.isEmpty() ? List.of("Indicative CO2e only — not a certified LCA.") : notes
        );
    }
}
