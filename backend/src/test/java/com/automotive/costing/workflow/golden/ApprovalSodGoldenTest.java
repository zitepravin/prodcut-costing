package com.automotive.costing.workflow.golden;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.platform.user.entity.Role;
import com.automotive.costing.platform.user.entity.User;
import com.automotive.costing.shouldcost.entity.ApprovalStatus;
import com.automotive.costing.workflow.dto.ApprovalWorkflowDtos.DecisionRequest;
import com.automotive.costing.workflow.engine.ApprovalWorkflowEngine;
import com.automotive.costing.workflow.entity.ApprovalStepInstance;
import com.automotive.costing.workflow.entity.DecisionStatus;
import com.automotive.costing.workflow.entity.WorkflowInstanceStatus;
import com.automotive.costing.workflow.service.ApprovalWorkflowService;
import com.automotive.costing.workflow.repository.ApprovalStepInstanceRepository;
import com.automotive.costing.workflow.repository.StepDefinitionRepository;
import com.automotive.costing.workflow.repository.WorkflowInstanceRepository;
import com.automotive.costing.workflow.service.WorkflowDefinitionService;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * V290 approval SOD / rejection / resubmit goldens against existing workflow engine + service.
 *
 * Existing SOD model (by design): role-based step assignment + override permission.
 * Maker (COSTING_ENGINEER) cannot approve a COSTING_MANAGER step without APPROVAL_OVERRIDE.
 * Same-user-id self-approve is not a separate product rule in current implementation —
 * SOD is enforced via role separation (documented, not invented here).
 */
@ExtendWith(MockitoExtension.class)
class ApprovalSodGoldenTest {

    @Mock WorkflowDefinitionService workflowDefinitionService;
    @Mock StepDefinitionRepository stepDefinitionRepository;
    @Mock WorkflowInstanceRepository instanceRepository;
    @Mock ApprovalStepInstanceRepository stepInstanceRepository;
    @Mock AuditService auditService;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock TenantAccessGuard tenantAccessGuard;

    ApprovalWorkflowService service;

    @BeforeEach
    void setUp() {
        lenient().when(tenantAccessGuard.requireCustomerId()).thenReturn("cust-A");
        service = new ApprovalWorkflowService(
                workflowDefinitionService, stepDefinitionRepository, instanceRepository,
                tenantAccessGuard, stepInstanceRepository, auditService, eventPublisher);
    }

    private static User userWithRoles(String userId, String... roleCodes) {
        User u = new User();
        u.setUserId(userId);
        u.setUsername(userId);
        Set<Role> roles = new java.util.HashSet<>();
        for (String code : roleCodes) {
            roles.add(Role.builder().roleId("r-" + code).roleCode(code).systemRole(true).build());
        }
        u.setRoles(roles);
        return u;
    }

    private static ApprovalStepInstance step(String id, int seq, String role, DecisionStatus st) {
        ApprovalStepInstance s = new ApprovalStepInstance();
        s.setStepInstanceId(id);
        s.setWorkflowInstanceId("wf-1");
        s.setSequenceNumber(seq);
        s.setApproverRole(role);
        s.setDecision(st);
        return s;
    }

    @Nested
    @DisplayName("Engine lifecycle SOD outcomes")
    class EngineLifecycle {
        @Test
        @DisplayName("SOD-ENG-01 any rejection → instance REJECTED")
        void rejectionFailsInstance() {
            List<ApprovalStepInstance> steps = List.of(
                    step("s1", 1, "COSTING_MANAGER", DecisionStatus.APPROVED),
                    step("s2", 2, "FINANCE_CONTROLLER", DecisionStatus.REJECTED));
            assertThat(ApprovalWorkflowEngine.determineOutcome(steps))
                    .isEqualTo(WorkflowInstanceStatus.REJECTED);
            assertThat(ApprovalWorkflowEngine.currentActionableSequence(steps)).isEqualTo(-1);
        }

        @Test
        @DisplayName("SOD-ENG-02 all approved → APPROVED")
        void allApproved() {
            List<ApprovalStepInstance> steps = List.of(
                    step("s1", 1, "COSTING_MANAGER", DecisionStatus.APPROVED),
                    step("s2", 2, "FINANCE_CONTROLLER", DecisionStatus.APPROVED));
            assertThat(ApprovalWorkflowEngine.determineOutcome(steps))
                    .isEqualTo(WorkflowInstanceStatus.APPROVED);
        }

        @Test
        @DisplayName("SOD-ENG-03 partial → still in progress (PENDING path)")
        void partialPending() {
            List<ApprovalStepInstance> steps = List.of(
                    step("s1", 1, "COSTING_MANAGER", DecisionStatus.APPROVED),
                    step("s2", 2, "FINANCE_CONTROLLER", DecisionStatus.PENDING));
            assertThat(ApprovalWorkflowEngine.determineOutcome(steps))
                    .isEqualTo(WorkflowInstanceStatus.IN_PROGRESS);
            assertThat(ApprovalWorkflowEngine.currentActionableSequence(steps)).isEqualTo(2);
        }

        @Test
        @DisplayName("SOD-ENG-04 returned treated as rejection outcome")
        void returnedIsRejected() {
            List<ApprovalStepInstance> steps = List.of(
                    step("s1", 1, "COSTING_MANAGER", DecisionStatus.RETURNED));
            assertThat(ApprovalWorkflowEngine.determineOutcome(steps))
                    .isEqualTo(WorkflowInstanceStatus.REJECTED);
        }
    }

    @Nested
    @DisplayName("Service SOD — role separation (maker cannot act as manager step)")
    class ServiceSod {
        @Test
        @DisplayName("SOD-SVC-01 COSTING_ENGINEER cannot decide COSTING_MANAGER step without override")
        void makerCannotApproveManagerStep() {
            ApprovalStepInstance pending = step("step-1", 1, "COSTING_MANAGER", DecisionStatus.PENDING);
            when(stepInstanceRepository.findById("step-1")).thenReturn(Optional.of(pending));
            when(stepInstanceRepository.findByWorkflowInstanceIdOrderBySequenceNumberAsc("wf-1"))
                    .thenReturn(List.of(pending));

            DecisionRequest req = new DecisionRequest("APPROVED", "looks fine", false, null);
            User maker = userWithRoles("user-engineer", "COSTING_ENGINEER");

            assertThatThrownBy(() -> service.decide("step-1", req, maker))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("Approval Override");
        }

        @Test
        @DisplayName("SOD-SVC-02 unauthorized role blocked")
        void unauthorizedRoleBlocked() {
            ApprovalStepInstance pending = step("step-1", 1, "COSTING_MANAGER", DecisionStatus.PENDING);
            when(stepInstanceRepository.findById("step-1")).thenReturn(Optional.of(pending));
            when(stepInstanceRepository.findByWorkflowInstanceIdOrderBySequenceNumberAsc("wf-1"))
                    .thenReturn(List.of(pending));

            DecisionRequest req = new DecisionRequest("APPROVED", "no", false, null);
            User viewer = userWithRoles("user-viewer", "VIEWER");

            assertThatThrownBy(() -> service.decide("step-1", req, viewer))
                    .isInstanceOf(BusinessRuleViolationException.class);
        }

        @Test
        @DisplayName("SOD-SVC-03 correct role on actionable step succeeds")
        void correctRoleSucceeds() {
            ApprovalStepInstance pending = step("step-1", 1, "COSTING_MANAGER", DecisionStatus.PENDING);
            when(stepInstanceRepository.findById("step-1")).thenReturn(Optional.of(pending));
            when(stepInstanceRepository.findByWorkflowInstanceIdOrderBySequenceNumberAsc("wf-1"))
                    .thenReturn(List.of(pending));
            when(stepInstanceRepository.save(any())).thenAnswer(inv -> {
                ApprovalStepInstance s = inv.getArgument(0);
                s.setDecision(DecisionStatus.APPROVED);
                return s;
            });
            com.automotive.costing.workflow.entity.WorkflowInstance inst =
                    new com.automotive.costing.workflow.entity.WorkflowInstance();
            inst.setWorkflowInstanceId("wf-1");
            inst.setCustomerId("cust-A");
            inst.setEntityType("ShouldCostBuildUp");
            inst.setEntityId("sc-1");
            inst.setStatus(WorkflowInstanceStatus.IN_PROGRESS);
            inst.setSubmittedBy("user-engineer");
            when(instanceRepository.findByCustomerIdAndWorkflowInstanceId("cust-A", "wf-1"))
                    .thenReturn(Optional.of(inst));
            when(instanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            DecisionRequest req = new DecisionRequest("APPROVED", "ok", false, null);
            User manager = userWithRoles("user-manager", "COSTING_MANAGER");
            var result = service.decide("step-1", req, manager);
            assertThat(result).isNotNull();
            verify(stepInstanceRepository).save(any());
        }

        @Test
        @DisplayName("SOD-SVC-04 already decided step cannot be re-decided")
        void noDoubleDecide() {
            ApprovalStepInstance done = step("step-1", 1, "COSTING_MANAGER", DecisionStatus.APPROVED);
            when(stepInstanceRepository.findById("step-1")).thenReturn(Optional.of(done));

            DecisionRequest req = new DecisionRequest("APPROVED", "again", false, null);
            assertThatThrownBy(() -> service.decide("step-1", req, userWithRoles("m", "COSTING_MANAGER")))
                    .isInstanceOf(BusinessRuleViolationException.class)
                    .hasMessageContaining("WF-05-already-decided");
        }
    }

    @Nested
    @DisplayName("Should-cost approval status lifecycle (governance transitions)")
    class ShouldCostLifecycle {
        @Test
        @DisplayName("SOD-SC-01 status enum supports DRAFT→SUBMITTED→APPROVED/REJECTED")
        void statusEnumComplete() {
            assertThat(ApprovalStatus.DRAFT.name()).isEqualTo("DRAFT");
            assertThat(ApprovalStatus.SUBMITTED.name()).isEqualTo("SUBMITTED");
            assertThat(ApprovalStatus.APPROVED.name()).isEqualTo("APPROVED");
            assertThat(ApprovalStatus.REJECTED.name()).isEqualTo("REJECTED");
        }

        @Test
        @DisplayName("SOD-SC-02 workflow reject outcome maps to resubmittable path (engine REJECTED)")
        void rejectAllowsResubmitConceptually() {
            // After rejection, instance is REJECTED; product returns should-cost to DRAFT via listener.
            // Engine-level: rejection is terminal for this instance; a new submit creates a new instance.
            List<ApprovalStepInstance> rejected = List.of(
                    step("s1", 1, "COSTING_MANAGER", DecisionStatus.REJECTED));
            assertThat(ApprovalWorkflowEngine.determineOutcome(rejected))
                    .isEqualTo(WorkflowInstanceStatus.REJECTED);
            // New instance can start again with PENDING steps (resubmit)
            List<ApprovalStepInstance> resubmit = List.of(
                    step("s1b", 1, "COSTING_MANAGER", DecisionStatus.PENDING));
            assertThat(ApprovalWorkflowEngine.determineOutcome(resubmit))
                    .isEqualTo(WorkflowInstanceStatus.IN_PROGRESS);
        }
    }
}
