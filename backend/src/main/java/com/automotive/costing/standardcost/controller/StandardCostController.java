package com.automotive.costing.standardcost.controller;

import com.automotive.costing.platform.access.RequiresModule;
import com.automotive.costing.standardcost.dto.StandardCostDtos.*;
import com.automotive.costing.standardcost.service.StandardCostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/standard-costs")
@RequiredArgsConstructor
@Tag(name = "Standard Cost Lifecycle (C1)")
@RequiresModule("PRODUCT_COSTING")
public class StandardCostController {

    private final StandardCostService service;

    @GetMapping
    public ResponseEntity<List<StandardCostResponse>> list(@RequestParam String bomNodeId) {
        return ResponseEntity.ok(service.listByBomNode(bomNodeId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<StandardCostResponse> get(@PathVariable String id) {
        return ResponseEntity.ok(service.get(id));
    }

    @GetMapping("/history")
    public ResponseEntity<LifecycleHistoryResponse> history(@RequestParam String bomNodeId) {
        return ResponseEntity.ok(service.history(bomNodeId));
    }

    @PostMapping("/from-should-cost")
    @PreAuthorize("hasAnyRole('COSTING_ENGINEER', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Create DRAFT standard cost snapshot from should-cost build-up")
    public ResponseEntity<StandardCostResponse> fromShouldCost(
            @Valid @RequestBody CreateFromShouldCostRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createFromShouldCost(request));
    }

    @PostMapping("/{id}/mark-for-release")
    @PreAuthorize("hasAnyRole('COSTING_ENGINEER', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<StandardCostResponse> markForRelease(@PathVariable String id) {
        return ResponseEntity.ok(service.markForRelease(id));
    }

    @PostMapping("/{id}/release")
    @PreAuthorize("hasAnyRole('COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Release standard cost; supersedes previous RELEASED versions")
    public ResponseEntity<StandardCostResponse> release(
            @PathVariable String id,
            Authentication authentication) {
        String user = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(service.release(id, user));
    }

    @PostMapping("/{id}/freeze")
    @PreAuthorize("hasAnyRole('COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<StandardCostResponse> freeze(
            @PathVariable String id,
            Authentication authentication) {
        String user = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(service.freeze(id, user));
    }

    @PostMapping("/{id}/supersede")
    @PreAuthorize("hasAnyRole('COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<StandardCostResponse> supersede(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(service.supersede(id, body.get("successorId")));
    }
}
