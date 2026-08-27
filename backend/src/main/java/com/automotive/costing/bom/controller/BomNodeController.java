package com.automotive.costing.bom.controller;

import com.automotive.costing.platform.access.RequiresModule;

import com.automotive.costing.bom.dto.BomNodeDtos.*;
import com.automotive.costing.bom.service.BomNodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** M02 BOM & Product Structure Management. See API_SPEC.md Section 4 / TDD Section 5.02. */
@RestController
@RequestMapping("/api/v1/bom")
@RequiredArgsConstructor
@Tag(name = "BOM & Product Structure")
@RequiresModule("PRODUCT_COSTING")
public class BomNodeController {

    private final BomNodeService service;

    @GetMapping("/{programId}/tree")
    @PreAuthorize("@permissionAuth.has('BOM_VIEW')")
    @Operation(summary = "Retrieve the full BOM tree for a program (BOM-01, BOM-06)")
    public ResponseEntity<List<TreeNode>> getTree(@PathVariable String programId) {
        return ResponseEntity.ok(service.getTree(programId));
    }

    @GetMapping("/nodes/{nodeId}")
    @PreAuthorize("@permissionAuth.has('BOM_VIEW')")
    @Operation(summary = "Get a single BOM node by ID")
    public ResponseEntity<Response> getById(@PathVariable String nodeId) {
        return ResponseEntity.ok(service.getById(nodeId));
    }

    @PostMapping("/{nodeId}/children")
    @PreAuthorize("@permissionAuth.has('BOM_CREATE')")
    @Operation(summary = "Add a child BOM node under the given parent (BOM-01)")
    public ResponseEntity<Response> addChild(@PathVariable String nodeId,
                                              @RequestParam String programId,
                                              @Valid @RequestBody CreateChildRequest request) {
        Response created = service.addChild(nodeId, programId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/roots")
    @PreAuthorize("@permissionAuth.has('BOM_CREATE')")
    @Operation(summary = "Create a root BOM node for a program (no parent)")
    public ResponseEntity<Response> addRoot(@RequestParam String programId,
                                             @Valid @RequestBody CreateChildRequest request) {
        Response created = service.addChild(null, programId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/nodes/{nodeId}")
    @PreAuthorize("@permissionAuth.has('BOM_EDIT')")
    @Operation(summary = "Update a BOM node's attributes")
    public ResponseEntity<Response> updateNode(@PathVariable String nodeId,
                                                @Valid @RequestBody UpdateNodeRequest request) {
        return ResponseEntity.ok(service.updateNode(nodeId, request));
    }

    @PutMapping("/nodes/{nodeId}/release")
    @PreAuthorize("@permissionAuth.has('BOM_EDIT')")
    @Operation(summary = "Release a BOM node - requires a linked ECN reference (SRS 4.2.5)")
    public ResponseEntity<Response> release(@PathVariable String nodeId) {
        return ResponseEntity.ok(service.release(nodeId));
    }

    @PostMapping("/nodes/{nodeId}/manual-override")
    @PreAuthorize("@permissionAuth.has('BOM_EDIT')")
    @Operation(summary = "Apply a manual cost override - requires an explicit justification (SRS 4.2.5)")
    public ResponseEntity<Response> applyManualOverride(@PathVariable String nodeId,
                                                          @Valid @RequestBody ManualOverrideRequest request) {
        return ResponseEntity.ok(service.applyManualOverride(nodeId, request));
    }

    @DeleteMapping("/nodes/{nodeId}/manual-override")
    @PreAuthorize("@permissionAuth.has('BOM_EDIT')")
    @Operation(summary = "Clear a manual cost override, reverting to computed roll-up cost")
    public ResponseEntity<Response> clearManualOverride(@PathVariable String nodeId) {
        return ResponseEntity.ok(service.clearManualOverride(nodeId));
    }

    @PostMapping("/{nodeId}/rollup")
    @PreAuthorize("@permissionAuth.has('BOM_EDIT')")
    @Operation(summary = "Trigger cost roll-up recalculation for a node's subtree (BOM-02)")
    public ResponseEntity<RollupTriggerResponse> triggerRollup(
            @PathVariable String nodeId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) LocalDate costingDate) {
        return ResponseEntity.ok(service.triggerRollup(nodeId, costingDate));
    }

    @PostMapping("/{programId}/rollup-program")
    @PreAuthorize("@permissionAuth.has('BOM_EDIT')")
    @Operation(summary = "Trigger cost roll-up recalculation for every root node in a program "
            + "(MAT-05: used automatically when a linked material rate is published, also "
            + "callable directly for a manual full-program recalculation)")
    public ResponseEntity<List<RollupTriggerResponse>> triggerRollupForProgram(
            @PathVariable String programId,
            @org.springframework.web.bind.annotation.RequestParam(required = false) LocalDate costingDate) {
        return ResponseEntity.ok(service.triggerRollupForProgram(programId, costingDate));
    }

    @PostMapping("/{nodeId}/what-if-branch")
    @PreAuthorize("@permissionAuth.has('BOM_CREATE')")
    @Operation(summary = "Create a what-if branch (subtree copy) without altering the released BOM (BOM-04)")
    public ResponseEntity<Response> createWhatIfBranch(@PathVariable String nodeId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createWhatIfBranch(nodeId));
    }

    @GetMapping("/{nodeId}/flatten")
    @PreAuthorize("@permissionAuth.has('BOM_VIEW')")
    @Operation(summary = "Flattened single-level raw-material export for a subtree (BOM-08)")
    public ResponseEntity<List<FlattenedLineItem>> flatten(@PathVariable String nodeId) {
        return ResponseEntity.ok(service.flatten(nodeId));
    }
}
