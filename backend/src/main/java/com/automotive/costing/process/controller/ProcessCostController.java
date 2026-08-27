package com.automotive.costing.process.controller;

import com.automotive.costing.platform.access.RequiresModule;

import com.automotive.costing.process.dto.ProcessCostDtos.*;
import com.automotive.costing.process.service.ProcessCostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** M04 Process &amp; Labor Costing - cost calculation. See TDD Section 5.04 / SRS Section 4.4. */
@RestController
@RequestMapping("/api/v1/costing/process")
@RequiredArgsConstructor
@Tag(name = "Process & Labor Costing")
@RequiresModule("PRODUCT_COSTING")
public class ProcessCostController {

    private final ProcessCostService processCostService;

    @PostMapping("/calculate")
    @PreAuthorize("hasAnyRole('PROCESS_ENGINEER', 'COSTING_ENGINEER', 'INDUSTRIAL_ENGINEER')")
    @Operation(summary = "Calculate process cost for a routing (PRC-02, PRC-04, PRC-06); triggers a program roll-up")
    public ResponseEntity<Response> calculate(@Valid @RequestBody CalculateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(processCostService.calculate(request));
    }

    @PostMapping("/compare")
    @Operation(summary = "Compare total process cost across two or more alternate routings (PRC-07)")
    public ResponseEntity<CompareResponse> compare(@Valid @RequestBody CompareRequest request) {
        return ResponseEntity.ok(processCostService.compareRoutings(request));
    }
}
