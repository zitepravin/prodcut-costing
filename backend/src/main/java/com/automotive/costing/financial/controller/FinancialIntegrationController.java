package com.automotive.costing.financial.controller;

import com.automotive.costing.financial.dto.FinancialDtos.*;
import com.automotive.costing.financial.service.ActualCostService;
import com.automotive.costing.financial.service.CostTriangleService;
import com.automotive.costing.financial.service.ValuationCurrencyService;
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
@RequestMapping("/api/v1/financial")
@RequiredArgsConstructor
@Tag(name = "Financial Integration C2–C4")
@RequiresModule("PRODUCT_COSTING")
public class FinancialIntegrationController {

    private final ActualCostService actualCostService;
    private final CostTriangleService costTriangleService;
    private final ValuationCurrencyService valuationCurrencyService;

    // --- C2 Actuals ---
    @GetMapping("/actual-costs")
    public ResponseEntity<List<ActualCostResponse>> listActuals(@RequestParam String bomNodeId) {
        return ResponseEntity.ok(actualCostService.list(bomNodeId));
    }

    @PostMapping("/actual-costs/import")
    @PreAuthorize("hasAnyRole('COSTING_ENGINEER', 'COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Import actual cost (never invents values)")
    public ResponseEntity<ActualCostResponse> importActual(@Valid @RequestBody ImportActualRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(actualCostService.importActual(request));
    }

    // --- C2/C3 Triangle ---
    @GetMapping("/cost-triangle")
    @Operation(summary = "Planned / Standard / Should / Actual / SAP comparison")
    public ResponseEntity<TriangleCompareResponse> triangle(
            @RequestParam String bomNodeId,
            @RequestParam(required = false) String reportingCurrency) {
        return ResponseEntity.ok(costTriangleService.compare(bomNodeId, reportingCurrency));
    }

    // --- C4 Valuation policy ---
    @GetMapping("/valuation-policies")
    public ResponseEntity<List<ValuationPolicyResponse>> listPolicies() {
        return ResponseEntity.ok(valuationCurrencyService.listActive());
    }

    @PostMapping("/valuation-policies")
    @PreAuthorize("hasAnyRole('COSTING_MANAGER', 'MASTER_DATA_ADMIN', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<ValuationPolicyResponse> createPolicy(
            @Valid @RequestBody CreateValuationPolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(valuationCurrencyService.create(request));
    }

    @PostMapping("/valuation-policies/convert")
    @Operation(summary = "Convert amount into company/group/costing currencies using policy valuation date")
    public ResponseEntity<ValuationConvertResponse> convert(
            @Valid @RequestBody ValuationConvertRequest request) {
        return ResponseEntity.ok(valuationCurrencyService.convert(request));
    }
}
