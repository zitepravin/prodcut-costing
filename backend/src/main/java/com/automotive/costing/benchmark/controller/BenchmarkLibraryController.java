package com.automotive.costing.benchmark.controller;

import com.automotive.costing.benchmark.dto.BenchmarkDtos.*;
import com.automotive.costing.benchmark.service.BenchmarkAnalyticsService;
import com.automotive.costing.benchmark.service.BenchmarkLibraryService;
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
@RequestMapping("/api/v1/benchmarks")
@RequiredArgsConstructor
@Tag(name = "Benchmark Knowledge Library")
@RequiresModule("PRODUCT_COSTING")
public class BenchmarkLibraryController {

    private final BenchmarkLibraryService service;
    private final BenchmarkAnalyticsService analyticsService;

    @GetMapping("/packs")
    @Operation(summary = "List benchmark packs")
    public ResponseEntity<List<PackResponse>> listPacks() {
        return ResponseEntity.ok(service.listPacks());
    }

    @GetMapping("/packs/{packId}")
    @Operation(summary = "Get pack with rates")
    public ResponseEntity<PackDetailResponse> getPack(@PathVariable String packId) {
        return ResponseEntity.ok(service.getPack(packId));
    }

    @PostMapping("/packs")
    @PreAuthorize("hasAnyRole('MASTER_DATA_ADMIN', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Create DRAFT benchmark pack")
    public ResponseEntity<PackResponse> createPack(@Valid @RequestBody CreatePackRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createPack(request));
    }

    @PostMapping("/packs/{packId}/publish")
    @PreAuthorize("hasAnyRole('MASTER_DATA_ADMIN', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Publish pack (requires ≥1 rate)")
    public ResponseEntity<PackResponse> publish(@PathVariable String packId) {
        return ResponseEntity.ok(service.publish(packId));
    }

    @PostMapping("/packs/{packId}/rates")
    @PreAuthorize("hasAnyRole('MASTER_DATA_ADMIN', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Add rate to pack")
    public ResponseEntity<RateResponse> addRate(
            @PathVariable String packId,
            @Valid @RequestBody CreateRateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addRate(packId, request));
    }

    @GetMapping("/analytics")
    @Operation(summary = "Should-cost vs quote vs benchmark analytics")
    public ResponseEntity<BenchmarkAnalyticsService.AnalyticsResponse> analytics(
            @RequestParam String bomNodeId,
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(analyticsService.analyze(bomNodeId, category));
    }
}
