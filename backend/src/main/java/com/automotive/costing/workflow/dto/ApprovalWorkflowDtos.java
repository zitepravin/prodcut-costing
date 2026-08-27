package com.automotive.costing.workflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class ApprovalWorkflowDtos {

    public record SubmitRequest(
            @NotBlank(message = "entityType is required") String entityType,
            @NotBlank(message = "entityId is required") String entityId,
            @NotNull(message = "value is required for routing") BigDecimal value,
            String commodityFamily,
            String plantCode
    ) {
    }

    public record DecisionRequest(
            @NotBlank(message = "decision is required") String decision, // APPROVED / REJECTED / RETURNED
            String comment,
            boolean overrideFlag,
            String overrideJustification
    ) {
    }

    public record StepResponse(
            String stepInstanceId,
            Integer sequenceNumber,
            String approverRole,
            String approverUserId,
            String decision,
            Instant decidedAt,
            String comment,
            Instant dueAt,
            boolean escalated,
            boolean overrideFlag,
            String overrideJustification,
            String workflowInstanceId,
            String entityType,
            String entityId
    ) {
    }

    public record InstanceResponse(
            String workflowInstanceId,
            String entityType,
            String entityId,
            String status,
            String submittedBy,
            Instant submittedAt,
            Instant completedAt,
            List<StepResponse> steps
    ) {
    }
}
