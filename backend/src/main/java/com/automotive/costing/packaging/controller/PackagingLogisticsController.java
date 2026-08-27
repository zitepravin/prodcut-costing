package com.automotive.costing.packaging.controller;

import com.automotive.costing.platform.access.RequiresModule;

import com.automotive.costing.packaging.dto.PackagingLogisticsDtos.*;
import com.automotive.costing.packaging.service.PackagingLogisticsCostService;
import com.automotive.costing.packaging.service.PackagingLogisticsProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * M07 Packaging & Logistics Costing API.
 * Profiles hold cost inputs; calculate produces append-only results consumed by Should-Cost.
 */
@RestController
@RequestMapping("/api/v1/packaging-logistics")
@RequiredArgsConstructor
@Tag(name = "Packaging & Logistics Costing")
@RequiresModule("PRODUCT_COSTING")
public class PackagingLogisticsController {

    private final PackagingLogisticsProfileService profileService;
    private final PackagingLogisticsCostService costService;

    @PostMapping("/profiles")
    @PreAuthorize("hasAnyRole('COSTING_ENGINEER', 'PURCHASING_BUYER', 'ADMIN')")
    @Operation(summary = "Create a packaging & logistics profile for a BOM node")
    public ResponseEntity<ProfileResponse> createProfile(@Valid @RequestBody CreateProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(profileService.create(request));
    }

    @PutMapping("/profiles/{profileId}")
    @PreAuthorize("hasAnyRole('COSTING_ENGINEER', 'PURCHASING_BUYER', 'ADMIN')")
    @Operation(summary = "Update a packaging & logistics profile")
    public ResponseEntity<ProfileResponse> updateProfile(
            @PathVariable String profileId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(profileService.update(profileId, request));
    }

    @GetMapping("/profiles/{profileId}")
    @Operation(summary = "Get packaging profile by id")
    public ResponseEntity<ProfileResponse> getProfile(@PathVariable String profileId) {
        return ResponseEntity.ok(profileService.getById(profileId));
    }

    @GetMapping("/profiles")
    @Operation(summary = "List packaging profiles for a BOM node")
    public ResponseEntity<List<ProfileResponse>> listByBomNode(@RequestParam String bomNodeId) {
        return ResponseEntity.ok(profileService.listByBomNode(bomNodeId));
    }

    @PostMapping("/profiles/{profileId}/deactivate")
    @PreAuthorize("hasAnyRole('COSTING_ENGINEER', 'ADMIN')")
    @Operation(summary = "Deactivate a packaging profile (soft)")
    public ResponseEntity<ProfileResponse> deactivate(@PathVariable String profileId) {
        return ResponseEntity.ok(profileService.deactivate(profileId));
    }

    @PostMapping("/calculate")
    @PreAuthorize("hasAnyRole('COSTING_ENGINEER', 'PURCHASING_BUYER', 'ADMIN')")
    @Operation(summary = "Calculate packaging + freight + duty per piece for a profile")
    public ResponseEntity<CalculationResponse> calculate(@Valid @RequestBody CalculateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(costService.calculate(request));
    }

    @GetMapping("/calculations/latest")
    @Operation(summary = "Latest packaging/logistics calculation for a BOM node")
    public ResponseEntity<CalculationResponse> latest(@RequestParam String bomNodeId) {
        return ResponseEntity.ok(costService.latestForBomNode(bomNodeId));
    }

    @GetMapping("/calculations")
    @Operation(summary = "Calculation history for a BOM node (newest first)")
    public ResponseEntity<List<CalculationResponse>> history(@RequestParam String bomNodeId) {
        return ResponseEntity.ok(costService.historyForBomNode(bomNodeId));
    }

    @PostMapping("/compare")
    @Operation(summary = "Compare total packaging+freight cost across up to five profiles")
    public ResponseEntity<CompareResponse> compare(@Valid @RequestBody CompareRequest request) {
        return ResponseEntity.ok(costService.compare(request));
    }
}
