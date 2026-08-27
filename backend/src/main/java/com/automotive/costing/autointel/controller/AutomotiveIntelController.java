package com.automotive.costing.autointel.controller;

import com.automotive.costing.autointel.dto.AutoIntelDtos.*;
import com.automotive.costing.autointel.service.AutomotiveIntelService;
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
@RequestMapping("/api/v1/auto-intel")
@RequiredArgsConstructor
@Tag(name = "Automotive Intelligence E3/E4")
@RequiresModule("PRODUCT_COSTING")
public class AutomotiveIntelController {

    private final AutomotiveIntelService service;

    @GetMapping("/ecn-impacts")
    public ResponseEntity<List<EcnImpactResponse>> listEcn(@RequestParam String bomNodeId) {
        return ResponseEntity.ok(service.listEcnByBom(bomNodeId));
    }

    @PostMapping("/ecn-impacts/analyze")
    @PreAuthorize("hasAnyRole('COSTING_ENGINEER', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "E3 ECN cost impact: baseline vs proposed → unit + annual program $")
    public ResponseEntity<EcnImpactResponse> analyzeEcn(@Valid @RequestBody AnalyzeEcnRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.analyzeEcn(request));
    }

    @GetMapping("/what-if")
    public ResponseEntity<List<WhatIfResponse>> listWhatIf(@RequestParam String bomNodeId) {
        return ResponseEntity.ok(service.listWhatIf(bomNodeId));
    }

    @PostMapping("/what-if/run")
    @PreAuthorize("hasAnyRole('COSTING_ENGINEER', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "E4 central what-if: material/process/tooling/FX/volume shocks")
    public ResponseEntity<WhatIfResponse> runWhatIf(@Valid @RequestBody RunWhatIfRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.runWhatIf(request));
    }
}
