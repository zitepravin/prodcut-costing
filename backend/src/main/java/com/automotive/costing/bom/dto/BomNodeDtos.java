package com.automotive.costing.bom.dto;

import com.automotive.costing.bom.entity.BomNodeStatus;
import com.automotive.costing.bom.entity.MakeBuyFlag;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class BomNodeDtos {

    public record CreateChildRequest(
            @NotBlank(message = "partNumber is required") String partNumber,
            String revision,
            @NotNull(message = "quantityPer is required")
            @DecimalMin(value = "0.0", inclusive = false, message = "quantityPer must be > 0")
            BigDecimal quantityPer,
            @NotBlank(message = "uom is required") String uom,
            MakeBuyFlag makeBuyFlag,
            String ecnReference,
            BigDecimal netWeight,
            BigDecimal scrapPct,
            String materialCode
    ) {
    }

    public record UpdateNodeRequest(
            String revision,
            BigDecimal quantityPer,
            String uom,
            MakeBuyFlag makeBuyFlag,
            String ecnReference,
            BigDecimal netWeight,
            BigDecimal scrapPct,
            String materialCode
    ) {
    }

    /** SRS 4.2.5: manual override requires an explicit flag AND a justification note. */
    public record ManualOverrideRequest(
            @NotNull(message = "overrideCost is required") BigDecimal overrideCost,
            @NotBlank(message = "justification is required to apply a manual cost override")
            String justification
    ) {
    }

    public record Response(
            String bomNodeId,
            String parentNodeId,
            String programId,
            String partNumber,
            String revision,
            BigDecimal quantityPer,
            String uom,
            MakeBuyFlag makeBuyFlag,
            String ecnReference,
            BigDecimal netWeight,
            BigDecimal scrapPct,
            String materialCode,
            BomNodeStatus status,
            boolean whatIfBranchFlag,
            BigDecimal rolledUpCost,
            boolean overrideFlag,
            String overrideJustification,
            String createdBy,
            Instant createdAt,
            String updatedBy,
            Instant updatedAt
    ) {
    }

    /** A single node with its children nested, for GET /api/v1/bom/{programId}/tree. */
    public record TreeNode(
            String bomNodeId,
            String partNumber,
            String revision,
            BigDecimal quantityPer,
            String uom,
            MakeBuyFlag makeBuyFlag,
            String materialCode,
            BomNodeStatus status,
            BigDecimal rolledUpCost,
            boolean overrideFlag,
            List<TreeNode> children
    ) {
    }

    public record RollupTriggerResponse(
            String rootNodeId,
            int nodesProcessed,
            int nodesOverridden,
            BigDecimal rootRolledUpCost
    ) {
    }

    public record FlattenedLineItem(
            String bomNodeId,
            String partNumber,
            String materialForm,
            BigDecimal totalQuantityConsumed,
            String uom
    ) {
    }
}
