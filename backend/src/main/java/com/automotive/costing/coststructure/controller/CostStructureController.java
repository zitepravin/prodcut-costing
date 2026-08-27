package com.automotive.costing.coststructure.controller;

import com.automotive.costing.coststructure.dto.CostStructureDtos.*;
import com.automotive.costing.coststructure.service.CostComponentStructureService;
import com.automotive.costing.coststructure.service.CostEstimateItemizationService;
import com.automotive.costing.coststructure.service.MultilevelCostedBomService;
import com.automotive.costing.platform.access.RequiresModule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cost-structure")
@RequiredArgsConstructor
@Tag(name = "Cost Structure A3–A5")
@RequiresModule("PRODUCT_COSTING")
public class CostStructureController {

    private final CostComponentStructureService componentStructureService;
    private final MultilevelCostedBomService multilevelCostedBomService;
    private final CostEstimateItemizationService itemizationService;

    @GetMapping("/components/hierarchy")
    @Operation(summary = "A3 hierarchical cost component structure")
    public ResponseEntity<ComponentStructureResponse> componentHierarchy() {
        return ResponseEntity.ok(componentStructureService.hierarchy());
    }

    @GetMapping("/costed-bom/{rootBomNodeId}")
    @Operation(summary = "A4 multilevel costed BOM tree")
    public ResponseEntity<MultilevelCostedBomResponse> costedBom(@PathVariable String rootBomNodeId) {
        return ResponseEntity.ok(multilevelCostedBomService.costedTree(rootBomNodeId));
    }

    @PostMapping("/itemization/should-cost/{buildUpId}")
    @Operation(summary = "A5 materialize itemization from should-cost build-up")
    public ResponseEntity<ItemizationResponse> materializeItemization(@PathVariable String buildUpId) {
        return ResponseEntity.ok(itemizationService.materializeFromShouldCost(buildUpId));
    }

    @GetMapping("/itemization/{refType}/{refId}")
    @Operation(summary = "A5 get itemization lines")
    public ResponseEntity<ItemizationResponse> getItemization(
            @PathVariable String refType,
            @PathVariable String refId) {
        return ResponseEntity.ok(itemizationService.get(refType, refId));
    }
}
