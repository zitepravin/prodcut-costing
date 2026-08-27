package com.automotive.costing.costingscenario.controller;

import com.automotive.costing.costingscenario.dto.CostingScenarioDtos.*;
import com.automotive.costing.costingscenario.service.CostingScenarioService;
import com.automotive.costing.platform.access.RequiresModule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/costing-scenarios")
@RequiredArgsConstructor
@Tag(name = "Costing Scenarios + Additive Costs (B4/B5)")
@RequiresModule("PRODUCT_COSTING")
public class CostingScenarioController {

    private final CostingScenarioService service;

    @GetMapping
    public ResponseEntity<List<ScenarioResponse>> list(@RequestParam String bomNodeId) {
        return ResponseEntity.ok(service.listByBomNode(bomNodeId));
    }

    @GetMapping("/{scenarioId}")
    public ResponseEntity<ScenarioDetailResponse> get(@PathVariable String scenarioId) {
        return ResponseEntity.ok(service.get(scenarioId));
    }

    @GetMapping("/compare")
    @Operation(summary = "Side-by-side scenario list for a BOM node")
    public ResponseEntity<ScenarioCompareResponse> compare(@RequestParam String bomNodeId) {
        return ResponseEntity.ok(service.compare(bomNodeId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('COSTING_ENGINEER', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<ScenarioResponse> create(@Valid @RequestBody CreateScenarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PostMapping("/branch")
    @PreAuthorize("hasAnyRole('COSTING_ENGINEER', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Branch a new DRAFT scenario from an existing one (never overwrites APPROVED)")
    public ResponseEntity<ScenarioResponse> branch(@Valid @RequestBody BranchScenarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.branch(request));
    }

    @PostMapping("/{scenarioId}/activate")
    @PreAuthorize("hasAnyRole('COSTING_ENGINEER', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<ScenarioResponse> activate(@PathVariable String scenarioId) {
        return ResponseEntity.ok(service.activate(scenarioId));
    }

    @PostMapping("/{scenarioId}/approve")
    @PreAuthorize("hasAnyRole('COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<ScenarioResponse> approve(@PathVariable String scenarioId) {
        return ResponseEntity.ok(service.approve(scenarioId));
    }

    @PostMapping("/{scenarioId}/link-should-cost")
    @PreAuthorize("hasAnyRole('COSTING_ENGINEER', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<ScenarioResponse> linkShouldCost(
            @PathVariable String scenarioId,
            @RequestBody Map<String, String> body) {
        String buildUpId = body.get("shouldCostBuildUpId");
        return ResponseEntity.ok(service.linkShouldCost(scenarioId, buildUpId));
    }

    @PostMapping("/{scenarioId}/additive-lines")
    @PreAuthorize("hasAnyRole('COSTING_ENGINEER', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "B5 add separately identifiable additive cost line")
    public ResponseEntity<AdditiveLineResponse> addAdditive(
            @PathVariable String scenarioId,
            @Valid @RequestBody AddAdditiveLineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addAdditiveLine(scenarioId, request));
    }
}
