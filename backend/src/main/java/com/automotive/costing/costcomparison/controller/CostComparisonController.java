package com.automotive.costing.costcomparison.controller;

import com.automotive.costing.costcomparison.dto.CostComparisonDtos.WorkspaceResponse;
import com.automotive.costing.costcomparison.service.CostComparisonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import com.automotive.costing.platform.access.RequiresModule;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cost-comparison")
@RequiredArgsConstructor
@Tag(name = "Cost Comparison Workspace")
@RequiresModule("PRODUCT_COSTING")
public class CostComparisonController {

    private final CostComparisonService costComparisonService;

    @GetMapping("/by-bom-node/{bomNodeId}")
    @Operation(summary = "Side-by-side workspace: should-cost scenarios, supplier quotes, tooling, targets for one BOM node")
    public ResponseEntity<WorkspaceResponse> workspace(@PathVariable String bomNodeId) {
        return ResponseEntity.ok(costComparisonService.workspaceForBomNode(bomNodeId));
    }
}
