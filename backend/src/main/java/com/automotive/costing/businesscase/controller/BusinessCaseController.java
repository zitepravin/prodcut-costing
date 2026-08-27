package com.automotive.costing.businesscase.controller;

import com.automotive.costing.businesscase.dto.BusinessCaseDtos.CreateScenarioRequest;
import com.automotive.costing.businesscase.dto.BusinessCaseDtos.ScenarioResponse;
import com.automotive.costing.businesscase.service.BusinessCaseService;
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
@RequestMapping("/api/v1/business-cases")
@RequiredArgsConstructor
@Tag(name = "Business Case / Profitability")
@RequiresModule("PRODUCT_COSTING")
public class BusinessCaseController {

    private final BusinessCaseService service;

    @GetMapping
    @Operation(summary = "List scenarios for a program")
    public ResponseEntity<List<ScenarioResponse>> list(@RequestParam String programId) {
        return ResponseEntity.ok(service.listByProgram(programId));
    }

    @GetMapping("/{scenarioId}")
    public ResponseEntity<ScenarioResponse> get(@PathVariable String scenarioId) {
        return ResponseEntity.ok(service.get(scenarioId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('COSTING_ENGINEER', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<ScenarioResponse> create(@Valid @RequestBody CreateScenarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PostMapping("/{scenarioId}/calculate")
    @PreAuthorize("hasAnyRole('COSTING_ENGINEER', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Calculate CM, NPV, payback (BigDecimal)")
    public ResponseEntity<ScenarioResponse> calculate(@PathVariable String scenarioId) {
        return ResponseEntity.ok(service.calculate(scenarioId));
    }
}
