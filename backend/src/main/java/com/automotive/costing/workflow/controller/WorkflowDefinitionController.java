package com.automotive.costing.workflow.controller;

import com.automotive.costing.workflow.dto.WorkflowDefinitionDtos.CreateRequest;
import com.automotive.costing.workflow.dto.WorkflowDefinitionDtos.Response;
import com.automotive.costing.workflow.service.WorkflowDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** M14 Quotation &amp; Costing Approval Workflow - definition configuration. See TDD 5.14 / SRS 4.14. */
@RestController
@RequestMapping("/api/v1/workflow/definitions")
@RequiredArgsConstructor
@Tag(name = "Approval Workflow Definitions")
public class WorkflowDefinitionController {

    private final WorkflowDefinitionService workflowDefinitionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'COSTING_MANAGER')")
    @Operation(summary = "Configure a multi-step approval workflow with routing rules (WF-01, WF-02)")
    public ResponseEntity<Response> create(@Valid @RequestBody CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(workflowDefinitionService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a workflow definition and its steps")
    public ResponseEntity<Response> getById(@PathVariable String id) {
        return ResponseEntity.ok(workflowDefinitionService.getById(id));
    }
}
