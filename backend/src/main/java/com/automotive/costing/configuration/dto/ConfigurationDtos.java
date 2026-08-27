package com.automotive.costing.configuration.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.List;

public final class ConfigurationDtos {
    private ConfigurationDtos() {}

    public record PackItemInput(@NotBlank String questionId, int sortOrder) {}

    public record CreatePackRequest(
            @NotBlank String packCode,
            @NotBlank String packName,
            String purpose,
            String description,
            List<PackItemInput> items
    ) {}

    public record PackResponse(
            String packId,
            String packCode,
            String packName,
            String purpose,
            String status,
            int versionNo,
            String description,
            List<PackItemInput> items,
            List<String> messages
    ) {}

    public record CostingDefaultsRequest(
            String defaultReportingCurrency,
            BigDecimal defaultScrapPercent,
            BigDecimal defaultOverheadPercent,
            String notes
    ) {}

    public record CostingDefaultsResponse(
            String defaultsId,
            String customerId,
            String defaultReportingCurrency,
            BigDecimal defaultScrapPercent,
            BigDecimal defaultOverheadPercent,
            String fxMissingPolicy,
            String releaseIncompletePolicy,
            String notes,
            List<String> policyNotes
    ) {}

    public record ModulePackInfo(
            String packCode,
            String packName,
            List<String> moduleCodes,
            String description
    ) {}

    public record EffectiveConfiguration(
            List<ModulePackInfo> availableModulePacks,
            CostingDefaultsResponse costingDefaults,
            List<PackResponse> activeQuestionnairePacks,
            List<String> policyNotes
    ) {}
}
