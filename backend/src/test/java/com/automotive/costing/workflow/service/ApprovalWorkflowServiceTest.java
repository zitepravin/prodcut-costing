package com.automotive.costing.workflow.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.platform.user.entity.Role;
import com.automotive.costing.platform.user.entity.User;
import com.automotive.costing.workflow.dto.ApprovalWorkflowDtos.DecisionRequest;
import com.automotive.costing.workflow.dto.ApprovalWorkflowDtos.InstanceResponse;
import com.automotive.costing.workflow.dto.ApprovalWorkflowDtos.SubmitRequest;
import com.automotive.costing.workflow.entity.*;
import com.automotive.costing.workflow.repository.ApprovalStepInstanceRepository;
import com.automotive.costing.workflow.repository.StepDefinitionRepository;
import com.automotive.costing.workflow.repository.WorkflowInstanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApprovalWorkflowServiceTest {

    @Mock private WorkflowDefinitionService workflowDefinitionService;
    @Mock TenantAccessGuard tenantAccessGuard;
    @Mock private StepDefinitionRepository stepDefinitionRepository;
    @Mock private WorkflowInstanceRepository instanceRepository;
    @Mock private ApprovalStepInstanceRepository stepInstanceRepository;
    @Mock private AuditService auditService;
    @Mock private ApplicationEventPublisher eventPublisher;

    private ApprovalWorkflowService service;

    @BeforeEach
    void setUp() {
        lenient().when(tenantAccessGuard.requireCustomerId()).thenReturn("cust-test-1");
        service = new ApprovalWorkflowService(workflowDefinitionService, stepDefinitionRepository, instanceRepository, tenantAccessGuard, stepInstanceRepository, auditService, eventPublisher);
    }

    private User userWithRole(String roleCode) {
        User user = new User();
        user.setUserId("user-1");
        user.setUsername("jsmith");
        user.setRoles(Set.of(Role.builder().roleId("r1").roleCode(roleCode).systemRole(true).build()));
        return user;
    }

    private WorkflowDefinition definition() {
        WorkflowDefinition d = new WorkflowDefinition();
        d.setWorkflowDefinitionId("def-1");
        d.setEntityType("ShouldCostBuildUp");
        d.setActive(true);
        return d;
    }

    private StepDefinition stepDef(int seq, String role, BigDecimal min, BigDecimal max) {
        StepDefinition sd = new StepDefinition();
        sd.setStepDefinitionId("sd-" + seq);
        sd.setSequenceNumber(seq);
        sd.setApproverRole(role);
        sd.setMinValue(min);
        sd.setMaxValue(max);
        return sd;
    }

    // WF-02 (Negative/Boundary): no active definition for the entity type at all.
    @Test
    void submit_withNoActiveDefinition_throwsBusinessRuleViolation() {
        when(workflowDefinitionService.activeDefinitionsFor("ShouldCostBuildUp")).thenReturn(List.of());

        SubmitRequest request = new SubmitRequest("ShouldCostBuildUp", "bu-1",
                new BigDecimal("50000"), null, null);

        assertThatThrownBy(() -> service.submit(request, userWithRole("COSTING_ENGINEER")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("WF-02");
    }

    // WF-02 (Negative/Boundary): a definition exists but no step's value range matches.
    @Test
    void submit_withNoMatchingStepRouting_throwsBusinessRuleViolation() {
        when(workflowDefinitionService.activeDefinitionsFor("ShouldCostBuildUp"))
                .thenReturn(List.of(definition()));
        when(workflowDefinitionService.stepsFor("def-1"))
                .thenReturn(List.of(stepDef(1, "COSTING_MANAGER", new BigDecimal("100000"), null)));

        SubmitRequest request = new SubmitRequest("ShouldCostBuildUp", "bu-1",
                new BigDecimal("5000"), null, null); // below the only step's minValue

        assertThatThrownBy(() -> service.submit(request, userWithRole("COSTING_ENGINEER")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("WF-02-no-matching-steps");
    }

    // WF-02 (Positive): a value within the matching step's range creates the instance.
    @Test
    void submit_withMatchingStepRouting_createsInstanceWithMatchingSteps() {
        when(workflowDefinitionService.activeDefinitionsFor("ShouldCostBuildUp"))
                .thenReturn(List.of(definition()));
        when(workflowDefinitionService.stepsFor("def-1"))
                .thenReturn(List.of(stepDef(1, "COSTING_MANAGER", null, new BigDecimal("100000"))));
        when(instanceRepository.save(any(WorkflowInstance.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stepInstanceRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        SubmitRequest request = new SubmitRequest("ShouldCostBuildUp", "bu-1",
                new BigDecimal("50000"), null, null);
        InstanceResponse result = service.submit(request, userWithRole("COSTING_ENGINEER"));

        assertThat(result.status()).isEqualTo("IN_PROGRESS");
        assertThat(result.steps()).hasSize(1);
        assertThat(result.steps().get(0).approverRole()).isEqualTo("COSTING_MANAGER");
    }

    private ApprovalStepInstance pendingStep(String id, int seq, String role) {
        ApprovalStepInstance s = new ApprovalStepInstance();
        s.setStepInstanceId(id);
        s.setWorkflowInstanceId("wi-1");
        s.setSequenceNumber(seq);
        s.setApproverRole(role);
        s.setDecision(DecisionStatus.PENDING);
        return s;
    }

    // SRS 4.14.5 (Negative/Boundary): a decision from the wrong role, without an override, is blocked.
    @Test
    void decide_withWrongRoleAndNoOverride_throwsBusinessRuleViolation() {
        ApprovalStepInstance step = pendingStep("step-1", 1, "COSTING_MANAGER");
        when(stepInstanceRepository.findById("step-1")).thenReturn(Optional.of(step));
        when(stepInstanceRepository.findByWorkflowInstanceIdOrderBySequenceNumberAsc("wi-1"))
                .thenReturn(List.of(step));

        DecisionRequest request = new DecisionRequest("APPROVED", "looks good", false, null);

        assertThatThrownBy(() -> service.decide("step-1", request, userWithRole("FINANCE_CONTROLLER")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("SRS-4.14.5-approval-override-required");
    }

    // SRS 4.14.5 (Negative/Boundary): override flagged but the user lacks the APPROVAL_OVERRIDE role.
    @Test
    void decide_withOverrideFlagButNoOverridePermission_throwsBusinessRuleViolation() {
        ApprovalStepInstance step = pendingStep("step-1", 1, "COSTING_MANAGER");
        when(stepInstanceRepository.findById("step-1")).thenReturn(Optional.of(step));
        when(stepInstanceRepository.findByWorkflowInstanceIdOrderBySequenceNumberAsc("wi-1"))
                .thenReturn(List.of(step));

        DecisionRequest request = new DecisionRequest("APPROVED", "emergency", true, "urgent business need");

        assertThatThrownBy(() -> service.decide("step-1", request, userWithRole("FINANCE_CONTROLLER")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("permission");
    }

    // Positive: the correct role deciding its own currently-actionable step succeeds without override.
    @Test
    void decide_withCorrectRoleOnActionableStep_succeedsWithoutOverride() {
        ApprovalStepInstance step = pendingStep("step-1", 1, "COSTING_MANAGER");
        when(stepInstanceRepository.findById("step-1")).thenReturn(Optional.of(step));
        when(stepInstanceRepository.findByWorkflowInstanceIdOrderBySequenceNumberAsc("wi-1"))
                .thenReturn(List.of(step));
        when(stepInstanceRepository.save(any(ApprovalStepInstance.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkflowInstance instance = new WorkflowInstance();
        instance.setWorkflowInstanceId("wi-1");
        instance.setEntityType("ShouldCostBuildUp");
        instance.setEntityId("bu-1");
        instance.setStatus(WorkflowInstanceStatus.IN_PROGRESS);
        when(instanceRepository.findByCustomerIdAndWorkflowInstanceId("cust-test-1", "wi-1")).thenReturn(Optional.of(instance));
        when(instanceRepository.save(any(WorkflowInstance.class))).thenAnswer(inv -> inv.getArgument(0));

        DecisionRequest request = new DecisionRequest("APPROVED", "approved", false, null);
        InstanceResponse result = service.decide("step-1", request, userWithRole("COSTING_MANAGER"));

        assertThat(result.status()).isEqualTo("APPROVED"); // single-step workflow, fully approved
        verify(eventPublisher).publishEvent(any(com.automotive.costing.common.event.WorkflowApprovedEvent.class));
    }

    // WF-05 (Negative/Boundary): a step that's already been decided cannot be decided again.
    @Test
    void decide_onAlreadyDecidedStep_throwsBusinessRuleViolation() {
        ApprovalStepInstance step = pendingStep("step-1", 1, "COSTING_MANAGER");
        step.setDecision(DecisionStatus.APPROVED);
        when(stepInstanceRepository.findById("step-1")).thenReturn(Optional.of(step));

        DecisionRequest request = new DecisionRequest("APPROVED", null, false, null);

        assertThatThrownBy(() -> service.decide("step-1", request, userWithRole("COSTING_MANAGER")))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("WF-05-already-decided");
    }
}
