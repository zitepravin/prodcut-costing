package com.automotive.costing.mixedcost.controller;

import com.automotive.costing.mixedcost.dto.MixedCostDtos.CreateRequest;
import com.automotive.costing.mixedcost.dto.MixedCostDtos.MixedCostResponse;
import com.automotive.costing.mixedcost.dto.MixedCostDtos.ReplaceLegsRequest;
import com.automotive.costing.mixedcost.service.MixedCostService;
import com.automotive.costing.platform.access.RequiresModule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Mixed Costing (P2-03)")
@RequiresModule("PRODUCT_COSTING")
public class MixedCostController {

    private final MixedCostService mixedCostService;

    @PostMapping("/api/v1/bom-nodes/{bomNodeId}/mixed-costs")
    @Operation(summary = "Create a mixed-cost version with legs")
    public ResponseEntity<MixedCostResponse> create(
            @PathVariable String bomNodeId,
            @Valid @RequestBody CreateRequest request
    ) {
        return ResponseEntity.ok(mixedCostService.create(bomNodeId, request));
    }

    @GetMapping("/api/v1/bom-nodes/{bomNodeId}/mixed-costs")
    @Operation(summary = "List mixed-cost versions for a BOM node")
    public ResponseEntity<List<MixedCostResponse>> list(@PathVariable String bomNodeId) {
        return ResponseEntity.ok(mixedCostService.listByBomNode(bomNodeId));
    }

    @GetMapping("/api/v1/mixed-costs/{mixedCostId}")
    @Operation(summary = "Get mixed-cost version detail")
    public ResponseEntity<MixedCostResponse> get(@PathVariable String mixedCostId) {
        return ResponseEntity.ok(mixedCostService.get(mixedCostId));
    }

    @PutMapping("/api/v1/mixed-costs/{mixedCostId}/legs")
    @Operation(summary = "Replace legs (DRAFT / pre-freeze only)")
    public ResponseEntity<MixedCostResponse> replaceLegs(
            @PathVariable String mixedCostId,
            @Valid @RequestBody ReplaceLegsRequest request
    ) {
        return ResponseEntity.ok(mixedCostService.replaceLegs(mixedCostId, request));
    }

    @PostMapping("/api/v1/mixed-costs/{mixedCostId}/calculate")
    @Operation(summary = "Resolve sources, check eligibility, compute blended unit cost")
    public ResponseEntity<MixedCostResponse> calculate(@PathVariable String mixedCostId) {
        return ResponseEntity.ok(mixedCostService.calculate(mixedCostId));
    }

    @PostMapping("/api/v1/mixed-costs/{mixedCostId}/freeze")
    @Operation(summary = "Freeze a CALCULATED version (snapshots immutable)")
    public ResponseEntity<MixedCostResponse> freeze(@PathVariable String mixedCostId) {
        return ResponseEntity.ok(mixedCostService.freeze(mixedCostId));
    }
}
