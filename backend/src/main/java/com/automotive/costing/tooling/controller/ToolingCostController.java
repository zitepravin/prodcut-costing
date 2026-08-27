package com.automotive.costing.tooling.controller;

import com.automotive.costing.platform.access.RequiresModule;

import com.automotive.costing.tooling.dto.ToolingCostDtos.*;
import com.automotive.costing.tooling.service.ToolingCostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** M06 Tooling, Die &amp; Fixture Investment Costing - cost calculation. See TDD Section 5.06 / SRS Section 4.6. */
@RestController
@RequestMapping("/api/v1/tooling")
@RequiredArgsConstructor
@Tag(name = "Tooling, Die & Fixture Investment Costing")
@RequiresModule("PRODUCT_COSTING")
public class ToolingCostController {

    private final ToolingCostService toolingCostService;

    @PostMapping("/calculate")
    @PreAuthorize("hasAnyRole('TOOLING_ENGINEER', 'COSTING_ENGINEER', 'PURCHASING_BUYER')")
    @Operation(summary = "Amortize a tooling record's cost per piece (TL-02, TL-04); triggers a program roll-up")
    public ResponseEntity<Response> calculate(@Valid @RequestBody CalculateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(toolingCostService.calculate(request));
    }

    @PostMapping("/compare")
    @Operation(summary = "Compare amortized tooling cost across up to five supplier proposals for the same part (TL-06)")
    public ResponseEntity<CompareResponse> compare(@Valid @RequestBody CompareRequest request) {
        return ResponseEntity.ok(toolingCostService.compare(request));
    }
}
