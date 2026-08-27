package com.automotive.costing.workflow.controller;

import com.automotive.costing.platform.user.entity.User;
import com.automotive.costing.workflow.dto.ApprovalWorkflowDtos.*;
import com.automotive.costing.workflow.service.ApprovalWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * M14 Quotation &amp; Costing Approval Workflow - running instances. See TDD 5.14 / SRS
 * 4.14. Deviates from TDD 5.14's literal {@code GET /api/v1/workflow/inbox/{userId}} path
 * - the inbox here is always "my inbox," resolved from the authenticated principal, not a
 * path parameter - exposing an arbitrary other user's pending-approvals list by ID in the
 * URL is a scope leak an approval inbox (which can reveal commercially sensitive pending
 * decisions) shouldn't have.
 */
@RestController
@RequestMapping("/api/v1/workflow")
@RequiredArgsConstructor
@Tag(name = "Approval Workflow")
public class ApprovalWorkflowController {

    private final ApprovalWorkflowService approvalWorkflowService;

    @PostMapping("/submit")
    @Operation(summary = "Submit a record for approval, routed by value/commodity/plant (WF-02)")
    public ResponseEntity<InstanceResponse> submit(@Valid @RequestBody SubmitRequest request,
                                                     @AuthenticationPrincipal User submitter) {
        return ResponseEntity.status(HttpStatus.CREATED).body(approvalWorkflowService.submit(request, submitter));
    }

    @PostMapping("/steps/{stepInstanceId}/decision")
    @Operation(summary = "Record an approve/reject/return decision on a step (WF-05)")
    public ResponseEntity<InstanceResponse> decide(@PathVariable String stepInstanceId,
                                                      @Valid @RequestBody DecisionRequest request,
                                                      @AuthenticationPrincipal User approver) {
        return ResponseEntity.ok(approvalWorkflowService.decide(stepInstanceId, request, approver));
    }

    @GetMapping("/inbox")
    @Operation(summary = "My pending approval inbox (WF-03)")
    public ResponseEntity<List<StepResponse>> inbox(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(approvalWorkflowService.inbox(user));
    }

    @GetMapping("/history")
    @Operation(summary = "Full approval history for a record (WF-07)")
    public ResponseEntity<List<InstanceResponse>> history(@RequestParam String entityType,
                                                             @RequestParam String entityId) {
        return ResponseEntity.ok(approvalWorkflowService.history(entityType, entityId));
    }
}
