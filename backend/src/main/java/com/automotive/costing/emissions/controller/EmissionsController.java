package com.automotive.costing.emissions.controller;

import com.automotive.costing.emissions.dto.EmissionsDtos.CreateFactorRequest;
import com.automotive.costing.emissions.dto.EmissionsDtos.EmissionsSnapshotResponse;
import com.automotive.costing.emissions.dto.EmissionsDtos.FactorResponse;
import com.automotive.costing.emissions.dto.EmissionsDtos.LinkMaterialFactorRequest;
import com.automotive.costing.emissions.service.EmissionFactorService;
import com.automotive.costing.emissions.service.ShouldCostEmissionsService;
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
@RequiredArgsConstructor
@Tag(name = "Emissions / CO2e (P3-11a)")
@RequiresModule("PRODUCT_COSTING")
public class EmissionsController {

    private final EmissionFactorService factorService;
    private final ShouldCostEmissionsService emissionsService;

    @GetMapping("/api/v1/emissions/factors")
    @Operation(summary = "List emission factors")
    public ResponseEntity<List<FactorResponse>> listFactors() {
        return ResponseEntity.ok(factorService.list());
    }

    @PostMapping("/api/v1/emissions/factors")
    @PreAuthorize("hasAnyRole('COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Create DRAFT emission factor")
    public ResponseEntity<FactorResponse> create(@Valid @RequestBody CreateFactorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(factorService.create(request));
    }

    @PostMapping("/api/v1/emissions/factors/{id}/publish")
    @PreAuthorize("hasAnyRole('COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<FactorResponse> publish(@PathVariable String id) {
        return ResponseEntity.ok(factorService.publish(id));
    }

    @PostMapping("/api/v1/emissions/factors/{id}/retire")
    @PreAuthorize("hasAnyRole('COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    public ResponseEntity<FactorResponse> retire(@PathVariable String id) {
        return ResponseEntity.ok(factorService.retire(id));
    }

    @PutMapping("/api/v1/materials/{materialRecordId}/emission-factor")
    @PreAuthorize("hasAnyRole('COSTING_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Link ACTIVE material emission factor as material default")
    public ResponseEntity<Void> linkMaterial(
            @PathVariable String materialRecordId,
            @Valid @RequestBody LinkMaterialFactorRequest request
    ) {
        factorService.linkMaterialDefault(materialRecordId, request.factorId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/should-cost/build-ups/{buildUpId}/emissions")
    @Operation(summary = "Get material CO2e snapshot for should-cost build-up")
    public ResponseEntity<EmissionsSnapshotResponse> getEmissions(@PathVariable String buildUpId) {
        return ResponseEntity.ok(emissionsService.getForBuildUp(buildUpId));
    }

    @PostMapping("/api/v1/should-cost/build-ups/{buildUpId}/emissions/compute")
    @Operation(summary = "Compute/refresh material CO2e snapshot (does not change money totals)")
    public ResponseEntity<EmissionsSnapshotResponse> compute(@PathVariable String buildUpId) {
        return ResponseEntity.ok(emissionsService.computeForBuildUp(buildUpId));
    }
}
