package com.automotive.costing.process.controller;

import com.automotive.costing.platform.access.RequiresModule;

import com.automotive.costing.process.dto.ProcessRoutingDtos.*;
import com.automotive.costing.process.service.ProcessRoutingService;
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
 * M04 Process &amp; Labor Costing - routing management. See TDD Section 5.04 / SRS Section
 * 4.4. Roles per SRS 4.4.7: Manufacturing/Process Engineer, Costing Engineer, Industrial
 * Engineer.
 */
@RestController
@RequestMapping("/api/v1/costing/process")
@RequiredArgsConstructor
@Tag(name = "Process & Labor Costing")
@RequiresModule("PRODUCT_COSTING")
public class ProcessRoutingController {

    private final ProcessRoutingService routingService;

    @PostMapping("/routing")
    @PreAuthorize("hasAnyRole('PROCESS_ENGINEER', 'COSTING_ENGINEER', 'INDUSTRIAL_ENGINEER')")
    @Operation(summary = "Create a process routing with its ordered operations (PRC-01)")
    public ResponseEntity<RoutingResponse> createRouting(@Valid @RequestBody CreateRoutingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(routingService.createRouting(request));
    }

    @PostMapping("/routing/{routingId}/operations")
    @PreAuthorize("hasAnyRole('PROCESS_ENGINEER', 'COSTING_ENGINEER', 'INDUSTRIAL_ENGINEER')")
    @Operation(summary = "Add an operation to an existing (DRAFT) routing (PRC-01)")
    public ResponseEntity<RoutingResponse> addOperation(@PathVariable String routingId,
                                                          @Valid @RequestBody OperationRequest request) {
        return ResponseEntity.ok(routingService.addOperation(routingId, request));
    }

    @PutMapping("/routing/{routingId}/approve")
    @PreAuthorize("hasAnyRole('PROCESS_ENGINEER', 'COSTING_ENGINEER', 'INDUSTRIAL_ENGINEER')")
    @Operation(summary = "Approve a routing - blocked if any operation is missing a machine/labor assignment (SRS 4.4.5)")
    public ResponseEntity<RoutingResponse> approve(@PathVariable String routingId) {
        return ResponseEntity.ok(routingService.approve(routingId));
    }

    @GetMapping("/routing/{routingId}")
    @Operation(summary = "Get a routing and its operations")
    public ResponseEntity<RoutingResponse> getRouting(@PathVariable String routingId) {
        return ResponseEntity.ok(routingService.getRouting(routingId));
    }

    @GetMapping("/routing/by-bom-node/{bomNodeId}")
    @Operation(summary = "All routings defined for a BOM node, for PRC-07 side-by-side comparison")
    public ResponseEntity<List<RoutingResponse>> getRoutingsForBomNode(@PathVariable String bomNodeId) {
        return ResponseEntity.ok(routingService.getRoutingsForBomNode(bomNodeId));
    }

    @GetMapping("/cycle-time-templates")
    @Operation(summary = "List built-in cycle-time formula references (PRC-03)")
    public ResponseEntity<List<CycleTimeTemplateResponse>> cycleTimeTemplates() {
        return ResponseEntity.ok(routingService.listCycleTimeTemplates());
    }
}
