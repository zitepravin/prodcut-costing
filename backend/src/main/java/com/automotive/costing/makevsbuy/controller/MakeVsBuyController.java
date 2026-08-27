package com.automotive.costing.makevsbuy.controller;

import com.automotive.costing.makevsbuy.dto.MakeVsBuyDtos.CreateRequest;
import com.automotive.costing.makevsbuy.dto.MakeVsBuyDtos.DecideRequest;
import com.automotive.costing.makevsbuy.dto.MakeVsBuyDtos.SimulationResponse;
import com.automotive.costing.makevsbuy.dto.MakeVsBuyDtos.SourcesRequest;
import com.automotive.costing.makevsbuy.dto.MakeVsBuyDtos.DecisionExportPackage;
import com.automotive.costing.makevsbuy.service.MakeVsBuyService;
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
@Tag(name = "Make vs Buy (P2-04)")
@RequiresModule("PRODUCT_COSTING")
public class MakeVsBuyController {

    private final MakeVsBuyService service;

    @PostMapping("/api/v1/bom-nodes/{bomNodeId}/make-vs-buy")
    @Operation(summary = "Create make-vs-buy simulation")
    public ResponseEntity<SimulationResponse> create(
            @PathVariable String bomNodeId,
            @Valid @RequestBody CreateRequest request
    ) {
        return ResponseEntity.ok(service.create(bomNodeId, request));
    }

    @GetMapping("/api/v1/bom-nodes/{bomNodeId}/make-vs-buy")
    @Operation(summary = "List simulations for BOM node")
    public ResponseEntity<List<SimulationResponse>> list(@PathVariable String bomNodeId) {
        return ResponseEntity.ok(service.listByBomNode(bomNodeId));
    }

    @GetMapping("/api/v1/make-vs-buy/{simulationId}")
    @Operation(summary = "Get simulation")
    public ResponseEntity<SimulationResponse> get(@PathVariable String simulationId) {
        return ResponseEntity.ok(service.get(simulationId));
    }

    @PutMapping("/api/v1/make-vs-buy/{simulationId}/sources")
    @Operation(summary = "Update MAKE/BUY/MIXED sources")
    public ResponseEntity<SimulationResponse> sources(
            @PathVariable String simulationId,
            @Valid @RequestBody SourcesRequest request
    ) {
        return ResponseEntity.ok(service.updateSources(simulationId, request));
    }

    @PostMapping("/api/v1/make-vs-buy/{simulationId}/compare")
    @Operation(summary = "Snapshot legs and produce draft recommendation")
    public ResponseEntity<SimulationResponse> compare(@PathVariable String simulationId) {
        return ResponseEntity.ok(service.compare(simulationId));
    }

    @PostMapping("/api/v1/make-vs-buy/{simulationId}/decide")
    @Operation(summary = "Human decision (reason required)")
    public ResponseEntity<SimulationResponse> decide(
            @PathVariable String simulationId,
            @Valid @RequestBody DecideRequest request
    ) {
        return ResponseEntity.ok(service.decide(simulationId, request));
    }

    @PostMapping("/api/v1/make-vs-buy/{simulationId}/apply-to-bom")
    @Operation(summary = "After DECIDED: apply MakeBuyFlag on BOM node (audited)")
    public ResponseEntity<SimulationResponse> applyToBom(@PathVariable String simulationId) {
        return ResponseEntity.ok(service.applyToBom(simulationId));
    }

    @GetMapping("/api/v1/make-vs-buy/{simulationId}/export-package")
    @Operation(summary = "P3-04: Export decision package for SAP/CPI (read-only JSON)")
    public ResponseEntity<DecisionExportPackage> exportPackage(@PathVariable String simulationId) {
        return ResponseEntity.ok(service.exportDecisionPackage(simulationId));
    }
}
