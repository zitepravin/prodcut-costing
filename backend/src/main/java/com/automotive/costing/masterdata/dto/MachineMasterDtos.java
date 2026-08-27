package com.automotive.costing.masterdata.dto;

import com.automotive.costing.masterdata.entity.MasterDataStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** OHD-01: machine rate build-up from depreciation, power, maintenance, floor-space components. */
public class MachineMasterDtos {

    public record CreateRequest(
            @NotBlank(message = "code is required") String code,
            String description,
            @NotNull(message = "rate is required") @DecimalMin(value = "0.0", message = "rate must be >= 0")
            BigDecimal rate,
            @NotBlank(message = "uom is required") String uom,
            String plantId,
            @NotNull(message = "effectiveFrom is required") LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String machineType,
            String capacity,
            BigDecimal depreciationLifeYears,
            BigDecimal capitalCost,
            BigDecimal annualOperatingHours,
            BigDecimal powerCostPerHour,
            BigDecimal maintenanceCostPerHour,
            BigDecimal floorSpaceCostPerHour,
            String shiftPattern
    ) {
    }

    public record UpdateRequest(
            String description,
            BigDecimal rate,
            String uom,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String machineType,
            String capacity,
            BigDecimal depreciationLifeYears,
            BigDecimal capitalCost,
            BigDecimal annualOperatingHours,
            BigDecimal powerCostPerHour,
            BigDecimal maintenanceCostPerHour,
            BigDecimal floorSpaceCostPerHour,
            String shiftPattern
    ) {
    }

    public record Response(
            String recordId, String code, String description, BigDecimal rate, String uom, String plantId,
            LocalDate effectiveFrom, LocalDate effectiveTo, Integer versionNumber, MasterDataStatus status,
            String machineType, String capacity, BigDecimal depreciationLifeYears, BigDecimal capitalCost,
            BigDecimal annualOperatingHours, BigDecimal powerCostPerHour, BigDecimal maintenanceCostPerHour,
            BigDecimal floorSpaceCostPerHour, String shiftPattern,
            String createdBy, Instant createdAt, String updatedBy, Instant updatedAt
    ) {
    }

    /** OHD-01: composed rate = (capitalCost / depreciationLifeYears / annualOperatingHours) + power + maintenance + floorSpace. */
    public record RateBuildUpResponse(
            String code,
            BigDecimal depreciationComponentPerHour,
            BigDecimal powerCostPerHour,
            BigDecimal maintenanceCostPerHour,
            BigDecimal floorSpaceCostPerHour,
            BigDecimal totalHourlyRate
    ) {
    }

    /**
     * OHD-06: "what-if simulation showing the effect of a change in capacity utilization
     * or capital cost on the resulting hourly rate." Every field is an optional override -
     * unsupplied fields fall back to the existing record's current value - so the caller
     * can simulate a single changed input (e.g. only annualOperatingHours, to model a
     * capacity-utilization change) without having to restate the whole rate build-up.
     * Never persisted - see {@code MachineMasterService.simulate()}.
     */
    public record SimulationRequest(
            BigDecimal depreciationLifeYears,
            BigDecimal capitalCost,
            BigDecimal annualOperatingHours,
            BigDecimal powerCostPerHour,
            BigDecimal maintenanceCostPerHour,
            BigDecimal floorSpaceCostPerHour
    ) {
    }

    public record SimulationResponse(
            String code,
            BigDecimal currentHourlyRate,
            BigDecimal simulatedHourlyRate,
            BigDecimal delta,
            RateBuildUpResponse simulatedBuildUp
    ) {
    }
}
