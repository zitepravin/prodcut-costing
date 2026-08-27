package com.automotive.costing.costingrun.controller;

import com.automotive.costing.costingrun.dto.CostingRunDtos.*;
import com.automotive.costing.costingrun.service.CostingRunService;
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

@RestController
@RequestMapping("/api/v1/costing-runs")
@RequiredArgsConstructor
@Tag(name = "Costing Run / Mass Costing (B1)")
@RequiresModule("PRODUCT_COSTING")
public class CostingRunController {

    private final CostingRunService service;

    @GetMapping
    public ResponseEntity<List<RunResponse>> list() {
        return ResponseEntity.ok(service.list());
    }

    @GetMapping("/{runId}")
    public ResponseEntity<RunDetailResponse> get(@PathVariable String runId) {
        return ResponseEntity.ok(service.get(runId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('COSTING_ENGINEER', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<RunDetailResponse> create(@Valid @RequestBody CreateRunRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PostMapping("/{runId}/execute")
    @PreAuthorize("hasAnyRole('COSTING_ENGINEER', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Execute mass quantity-structure costing for all items")
    public ResponseEntity<RunDetailResponse> execute(@PathVariable String runId) {
        return ResponseEntity.ok(service.execute(runId));
    }

    @PostMapping("/{runId}/rerun-failed")
    @PreAuthorize("hasAnyRole('COSTING_ENGINEER', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<RunDetailResponse> rerunFailed(@PathVariable String runId) {
        return ResponseEntity.ok(service.rerunFailed(runId));
    }
}
