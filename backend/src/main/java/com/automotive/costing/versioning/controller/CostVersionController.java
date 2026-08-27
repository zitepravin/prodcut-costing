package com.automotive.costing.versioning.controller;

import com.automotive.costing.versioning.dto.VersionControlDtos.CompareResponse;
import com.automotive.costing.versioning.dto.VersionControlDtos.CreateSnapshotRequest;
import com.automotive.costing.versioning.dto.VersionControlDtos.TimelineEntry;
import com.automotive.costing.versioning.dto.VersionControlDtos.VersionResponse;
import com.automotive.costing.versioning.service.CostVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.automotive.costing.platform.access.RequiresModule;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/versions")
@RequiredArgsConstructor
@Tag(name = "Version Control (M15)")
@RequiresModule("PRODUCT_COSTING")
public class CostVersionController {

    private final CostVersionService costVersionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('COSTING_ENGINEER', 'COSTING_MANAGER', 'PROGRAM_COST_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Create immutable cost version snapshot (VER-01/VER-02)")
    public ResponseEntity<VersionResponse> create(@Valid @RequestBody CreateSnapshotRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(costVersionService.createSnapshot(request));
    }

    @GetMapping("/timeline")
    @Operation(summary = "Version timeline for an entity (VER-06)")
    public ResponseEntity<List<TimelineEntry>> timeline(
            @RequestParam String entityType,
            @RequestParam String entityId) {
        return ResponseEntity.ok(costVersionService.timeline(entityType, entityId));
    }

    @GetMapping("/{versionId}")
    @Operation(summary = "Get one version including snapshot payload")
    public ResponseEntity<VersionResponse> get(@PathVariable String versionId) {
        return ResponseEntity.ok(costVersionService.getVersion(versionId));
    }

    @PostMapping("/{versionId}/baseline")
    @PreAuthorize("hasAnyRole('COSTING_MANAGER', 'PROGRAM_COST_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Designate version as Program Baseline (VER-04)")
    public ResponseEntity<VersionResponse> baseline(@PathVariable String versionId) {
        return ResponseEntity.ok(costVersionService.designateBaseline(versionId));
    }

    @PostMapping("/{versionId}/rollback")
    @PreAuthorize("hasAnyRole('COSTING_ENGINEER', 'COSTING_MANAGER', 'PROGRAM_COST_MANAGER', 'SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Rollback to prior version by creating a new version (VER-05)")
    public ResponseEntity<VersionResponse> rollback(
            @PathVariable String versionId,
            @RequestBody(required = false) Map<String, String> body) {
        String comment = body != null ? body.get("comment") : null;
        return ResponseEntity.ok(costVersionService.rollback(versionId, comment));
    }

    @GetMapping("/compare")
    @Operation(summary = "Compare two historical versions (VER-03)")
    public ResponseEntity<CompareResponse> compare(
            @RequestParam String entityType,
            @RequestParam String entityId,
            @RequestParam int leftVersion,
            @RequestParam int rightVersion) {
        return ResponseEntity.ok(costVersionService.compare(entityType, entityId, leftVersion, rightVersion));
    }
}
