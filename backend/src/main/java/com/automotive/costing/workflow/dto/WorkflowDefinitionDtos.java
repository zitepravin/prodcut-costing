package com.automotive.costing.workflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public class WorkflowDefinitionDtos {

    public record StepDefinitionRequest(
            @NotNull(message = "sequenceNumber is required") Integer sequenceNumber,
            @NotBlank(message = "approverRole is required") String approverRole,
            BigDecimal minValue,
            BigDecimal maxValue,
            String commodityFamily,
            String plantCode,
            Integer escalationHours
    ) {
    }

    public record CreateRequest(
            @NotBlank(message = "name is required") String name,
            @NotBlank(message = "entityType is required") String entityType,
            @NotEmpty(message = "at least one step is required") @Valid List<StepDefinitionRequest> steps
    ) {
    }

    public record StepDefinitionResponse(
            String stepDefinitionId,
            Integer sequenceNumber,
            String approverRole,
            BigDecimal minValue,
            BigDecimal maxValue,
            String commodityFamily,
            String plantCode,
            Integer escalationHours
    ) {
    }

    public record Response(
            String workflowDefinitionId,
            String name,
            String entityType,
            boolean active,
            List<StepDefinitionResponse> steps
    ) {
    }
}
