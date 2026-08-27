package com.automotive.costing.workflow.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.event.WorkflowApprovedEvent;
import com.automotive.costing.common.event.WorkflowRejectedEvent;
import com.automotive.costing.common.event.WorkflowSubmittedEvent;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.platform.user.entity.Role;
import com.automotive.costing.platform.user.entity.User;
import com.automotive.costing.workflow.dto.ApprovalWorkflowDtos.*;
import com.automotive.costing.workflow.engine.ApprovalWorkflowEngine;
import com.automotive.costing.workflow.entity.*;
import com.automotive.costing.workflow.repository.ApprovalStepInstanceRepository;
import com.automotive.costing.workflow.repository.StepDefinitionRepository;
import com.automotive.costing.workflow.repository.WorkflowInstanceRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * M14 Quotation &amp; Costing Approval Workflow - the running-instance half (WF-03/04/05/
 * 06/07). {@code WorkflowDefinitionService} owns the templates this reads from (WF-01/02).
 *
 * SRS 4.14.5's "Approval Override" permission is checked against a dedicated
 * {@code APPROVAL_OVERRIDE} role (seeded in V10, alongside the 8+ predefined roles from
 * earlier sessions) rather than folding it into an existing job-title role like Costing
 * Manager - the SRS phrases it as "an explicit... permission," which reads as something
 * granted deliberately and separately from a person's normal job-function role, the same
 * way a real delegation-of-authority policy would treat it.
 */
@Service
@RequiredArgsConstructor
public class ApprovalWorkflowService {

    private static final String ENTITY_TYPE = "WorkflowInstance";
    private static final String APPROVAL_OVERRIDE_ROLE = "APPROVAL_OVERRIDE";

    private final WorkflowDefinitionService workflowDefinitionService;
    private final StepDefinitionRepository stepDefinitionRepository;
    private final WorkflowInstanceRepository instanceRepository;
    private final TenantAccessGuard tenantAccessGuard;
    private final ApprovalStepInstanceRepository stepInstanceRepository;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    /** WF-02: resolves the applicable workflow definition and routes to matching steps, then creates the instance. */
    @Transactional
    public InstanceResponse submit(SubmitRequest request, User submitter) {
        var definitions = workflowDefinitionService.activeDefinitionsFor(request.entityType());
        if (definitions.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "No active workflow definition configured for entityType '" + request.entityType() + "'.",
                    "WF-02-no-definition");
        }
        // MVP: the first active definition for this entity type. Multiple concurrently
        // active definitions per entity type (e.g. different definitions for different
        // business units) would need an additional disambiguation rule - not built this
        // session, flagged in NEXT_AGENT.md.
        var definition = definitions.get(0);
        var allStepDefs = workflowDefinitionService.stepsFor(definition.getWorkflowDefinitionId());

        List<StepDefinition> matchingSteps = allStepDefs.stream()
                .filter(s -> matchesRouting(s, request))
                .toList();
        if (matchingSteps.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "No workflow steps matched this submission's value/commodity/plant - check the "
                            + "workflow definition's routing rules.", "WF-02-no-matching-steps");
        }

        WorkflowInstance instance = new WorkflowInstance();
        instance.setCustomerId(tenantAccessGuard.requireCustomerId());
        instance.setWorkflowInstanceId(UUID.randomUUID().toString());
        instance.setWorkflowDefinitionId(definition.getWorkflowDefinitionId());
        instance.setEntityType(request.entityType());
        instance.setEntityId(request.entityId());
        instance.setStatus(WorkflowInstanceStatus.IN_PROGRESS);
        instance.setSubmittedBy(submitter.getUserId());
        instance.setSubmittedAt(Instant.now());
        WorkflowInstance savedInstance = instanceRepository.save(instance);

        List<ApprovalStepInstance> stepInstances = matchingSteps.stream()
                .map(sd -> toStepInstance(savedInstance.getWorkflowInstanceId(), sd))
                .toList();
        List<ApprovalStepInstance> savedSteps = stepInstanceRepository.saveAll(stepInstances);

        auditService.recordCreate(ENTITY_TYPE, savedInstance.getWorkflowInstanceId(),
                "submitted for " + request.entityType() + "/" + request.entityId()
                        + " with " + savedSteps.size() + " step(s)");
        eventPublisher.publishEvent(new WorkflowSubmittedEvent(
                request.entityType(), request.entityId(), savedInstance.getWorkflowInstanceId()));
        return toResponse(savedInstance, savedSteps);
    }

    /**
     * WF-05: record a decision. SRS 4.14.5's override rule is enforced here: acting out of
     * turn (not the currently-actionable sequence) or under a different role than the step
     * requires is blocked UNLESS overrideFlag is set, a justification is supplied, AND the
     * user holds the APPROVAL_OVERRIDE role.
     */
    @Transactional
    public InstanceResponse decide(String stepInstanceId, DecisionRequest request, User approver) {
        ApprovalStepInstance step = stepInstanceRepository.findById(stepInstanceId)
                .orElseThrow(() -> ResourceNotFoundException.of("ApprovalStepInstance", stepInstanceId));
        if (step.getDecision() != DecisionStatus.PENDING) {
            throw new BusinessRuleViolationException(
                    "Step " + stepInstanceId + " has already been decided (" + step.getDecision() + ").",
                    "WF-05-already-decided");
        }

        List<ApprovalStepInstance> allSteps = stepInstanceRepository
                .findByWorkflowInstanceIdOrderBySequenceNumberAsc(step.getWorkflowInstanceId());
        boolean isCurrentlyActionable = ApprovalWorkflowEngine.currentActionableSteps(allSteps).stream()
                .anyMatch(s -> s.getStepInstanceId().equals(stepInstanceId));
        boolean approverHasRequiredRole = hasRole(approver, step.getApproverRole());

        boolean needsOverride = !isCurrentlyActionable || !approverHasRequiredRole;
        if (needsOverride) {
            requireValidOverride(request, approver);
        }

        DecisionStatus decision = parseDecision(request.decision());
        step.setDecision(decision);
        step.setApproverUserId(approver.getUserId());
        step.setDecidedAt(Instant.now());
        step.setComment(request.comment());
        if (request.overrideFlag()) {
            step.setOverrideFlag(true);
            step.setOverrideJustification(request.overrideJustification());
        }
        ApprovalStepInstance savedStep = stepInstanceRepository.save(step);

        List<ApprovalStepInstance> updatedSteps = allSteps.stream()
                .map(s -> s.getStepInstanceId().equals(stepInstanceId) ? savedStep : s)
                .toList();
        WorkflowInstanceStatus outcome = ApprovalWorkflowEngine.determineOutcome(updatedSteps);

        WorkflowInstance instance = instanceRepository.findByCustomerIdAndWorkflowInstanceId(tenantAccessGuard.requireCustomerId(), step.getWorkflowInstanceId())
                .orElseThrow(() -> ResourceNotFoundException.of("WorkflowInstance", step.getWorkflowInstanceId()));
        if (outcome != WorkflowInstanceStatus.IN_PROGRESS) {
            instance.setStatus(outcome);
            instance.setCompletedAt(Instant.now());
            instanceRepository.save(instance);
            if (outcome == WorkflowInstanceStatus.APPROVED) {
                eventPublisher.publishEvent(new WorkflowApprovedEvent(
                        instance.getEntityType(), instance.getEntityId(), instance.getWorkflowInstanceId()));
            } else if (outcome == WorkflowInstanceStatus.REJECTED) {
                eventPublisher.publishEvent(new WorkflowRejectedEvent(
                        instance.getEntityType(), instance.getEntityId(), instance.getWorkflowInstanceId()));
            }
        }

        auditService.recordUpdate(ENTITY_TYPE, instance.getWorkflowInstanceId(), null,
                "step " + stepInstanceId + " -> " + decision
                        + (needsOverride ? " [OVERRIDE by " + approver.getUsername() + "]" : ""));

        return toResponse(instance, updatedSteps);
    }

    /** WF-03: pending steps this user can act on right now (role-eligible AND currently actionable in sequence). */
    @Transactional(readOnly = true)
    public List<StepResponse> inbox(User user) {
        List<String> roleCodes = user.getRoles().stream().map(Role::getRoleCode).toList();
        List<ApprovalStepInstance> byRole =
                stepInstanceRepository.findByApproverRoleInAndDecision(roleCodes, DecisionStatus.PENDING);
        // WF-04: also surface steps explicitly reassigned to this user after escalation
        List<ApprovalStepInstance> byAssignee =
                stepInstanceRepository.findByApproverUserIdAndDecision(user.getUserId(), DecisionStatus.PENDING);

        Map<String, ApprovalStepInstance> merged = new LinkedHashMap<>();
        for (ApprovalStepInstance s : byRole) {
            merged.put(s.getStepInstanceId(), s);
        }
        for (ApprovalStepInstance s : byAssignee) {
            merged.put(s.getStepInstanceId(), s);
        }

        return merged.values().stream()
                .filter(step -> {
                    List<ApprovalStepInstance> siblingSteps = stepInstanceRepository
                            .findByWorkflowInstanceIdOrderBySequenceNumberAsc(step.getWorkflowInstanceId());
                    return ApprovalWorkflowEngine.currentActionableSteps(siblingSteps).stream()
                            .anyMatch(s -> s.getStepInstanceId().equals(step.getStepInstanceId()));
                })
                .map(this::toStepResponse)
                .toList();
    }

    /** WF-07: full, immutable history for a record. */
    @Transactional(readOnly = true)
    public List<InstanceResponse> history(String entityType, String entityId) {
        return instanceRepository.findByCustomerIdAndEntityTypeAndEntityIdOrderBySubmittedAtDesc(tenantAccessGuard.requireCustomerId(), entityType, entityId).stream()
                .map(instance -> toResponse(instance,
                        stepInstanceRepository.findByWorkflowInstanceIdOrderBySequenceNumberAsc(
                                instance.getWorkflowInstanceId())))
                .toList();
    }

    /** WF-06: "prevent a cost record from being used as an official baseline... until all required approval steps are complete." */
    @Transactional(readOnly = true)
    public boolean hasApprovedWorkflow(String entityType, String entityId) {
        return instanceRepository.existsByEntityTypeAndEntityIdAndStatus(
                entityType, entityId, WorkflowInstanceStatus.APPROVED);
    }

    private boolean matchesRouting(StepDefinition s, SubmitRequest request) {
        boolean valueOk = (s.getMinValue() == null || request.value().compareTo(s.getMinValue()) >= 0)
                && (s.getMaxValue() == null || request.value().compareTo(s.getMaxValue()) <= 0);
        boolean commodityOk = s.getCommodityFamily() == null || s.getCommodityFamily().equals(request.commodityFamily());
        boolean plantOk = s.getPlantCode() == null || s.getPlantCode().equals(request.plantCode());
        return valueOk && commodityOk && plantOk;
    }

    private void requireValidOverride(DecisionRequest request, User approver) {
        if (!request.overrideFlag() || request.overrideJustification() == null
                || request.overrideJustification().isBlank()) {
            throw new BusinessRuleViolationException(
                    "This decision is out of turn or outside your assigned role for this step - "
                            + "an Approval Override with a justification is required (SRS 4.14.5).",
                    "SRS-4.14.5-approval-override-required");
        }
        if (!hasRole(approver, APPROVAL_OVERRIDE_ROLE)) {
            throw new BusinessRuleViolationException(
                    "User '" + approver.getUsername() + "' does not hold the Approval Override permission.",
                    "SRS-4.14.5-approval-override-permission-required");
        }
    }

    private boolean hasRole(User user, String roleCode) {
        return user.getRoles().stream().anyMatch(r -> r.getRoleCode().equals(roleCode));
    }

    private DecisionStatus parseDecision(String raw) {
        try {
            DecisionStatus decision = DecisionStatus.valueOf(raw.toUpperCase());
            if (decision == DecisionStatus.PENDING) {
                throw new IllegalArgumentException();
            }
            return decision;
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleViolationException(
                    "decision must be one of APPROVED, REJECTED, RETURNED.", "WF-05");
        }
    }

    private ApprovalStepInstance toStepInstance(String workflowInstanceId, StepDefinition sd) {
        ApprovalStepInstance step = new ApprovalStepInstance();
        step.setStepInstanceId(UUID.randomUUID().toString());
        step.setWorkflowInstanceId(workflowInstanceId);
        step.setSequenceNumber(sd.getSequenceNumber());
        step.setApproverRole(sd.getApproverRole());
        step.setDecision(DecisionStatus.PENDING);
        step.setEscalated(false);
        step.setOverrideFlag(false);
        if (sd.getEscalationHours() != null) {
            step.setDueAt(Instant.now().plus(sd.getEscalationHours(), ChronoUnit.HOURS));
        }
        return step;
    }

    private InstanceResponse toResponse(WorkflowInstance instance, List<ApprovalStepInstance> steps) {
        return new InstanceResponse(instance.getWorkflowInstanceId(), instance.getEntityType(),
                instance.getEntityId(), instance.getStatus().name(), instance.getSubmittedBy(),
                instance.getSubmittedAt(), instance.getCompletedAt(),
                steps.stream().map(this::toStepResponse).toList());
    }

    private StepResponse toStepResponse(ApprovalStepInstance s) {
        WorkflowInstance parent = instanceRepository.findByCustomerIdAndWorkflowInstanceId(tenantAccessGuard.requireCustomerId(), s.getWorkflowInstanceId()).orElse(null);
        String entityType = parent != null ? parent.getEntityType() : null;
        String entityId = parent != null ? parent.getEntityId() : null;
        return new StepResponse(s.getStepInstanceId(), s.getSequenceNumber(), s.getApproverRole(),
                s.getApproverUserId(), s.getDecision().name(), s.getDecidedAt(), s.getComment(), s.getDueAt(),
                s.isEscalated(), s.isOverrideFlag(), s.getOverrideJustification(),
                s.getWorkflowInstanceId(), entityType, entityId);
    }
}
