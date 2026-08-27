package com.automotive.costing.masterdata.dto;

import com.automotive.costing.masterdata.entity.MasterDataStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Request/response DTOs for Material Master. Controllers/services must never expose
 * the MaterialMaster JPA entity directly, per CLAUDE.md's "never expose entities" rule.
 */
public class MaterialMasterDtos {

    /** MDM-05: density, rate, and uom are mandatory - activation is blocked without them. */
    public record CreateRequest(
            @NotBlank(message = "code is required") String code,
            String description,
            @NotNull(message = "rate is required") @DecimalMin(value = "0.0", inclusive = true,
                    message = "rate must be >= 0") BigDecimal rate,
            @NotBlank(message = "uom is required") String uom,
            String plantId,
            @NotNull(message = "effectiveFrom is required") LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String grade,
            String specificationStandard,
            @NotNull(message = "density is required") @DecimalMin(value = "0.0", inclusive = false,
                    message = "density must be > 0") BigDecimal density,
            BigDecimal scrapRecoveryRate,
            String materialForm,
            boolean indexLinkedFlag,
            String indexCode,
            BigDecimal basePrice,
            BigDecimal alloySurchargePerUnit
    ) {
    }

    public record UpdateRequest(
            String description,
            @DecimalMin(value = "0.0", inclusive = true, message = "rate must be >= 0") BigDecimal rate,
            String uom,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String grade,
            String specificationStandard,
            BigDecimal density,
            BigDecimal scrapRecoveryRate,
            String materialForm,
            Boolean indexLinkedFlag,
            String indexCode,
            BigDecimal basePrice,
            BigDecimal alloySurchargePerUnit
    ) {
    }

    public record Response(
            String recordId,
            String code,
            String description,
            BigDecimal rate,
            String uom,
            String plantId,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            Integer versionNumber,
            MasterDataStatus status,
            String grade,
            String specificationStandard,
            BigDecimal density,
            BigDecimal scrapRecoveryRate,
            String materialForm,
            boolean indexLinkedFlag,
            String indexCode,
            BigDecimal basePrice,
            BigDecimal alloySurchargePerUnit,
            String createdBy,
            Instant createdAt,
            String updatedBy,
            Instant updatedAt
    ) {
    }
}
