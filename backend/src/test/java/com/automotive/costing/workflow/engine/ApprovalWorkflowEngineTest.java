package com.automotive.costing.workflow.engine;

import com.automotive.costing.workflow.entity.ApprovalStepInstance;
import com.automotive.costing.workflow.entity.DecisionStatus;
import com.automotive.costing.workflow.entity.WorkflowInstanceStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ApprovalWorkflowEngine, mapped to QA_Test_Case_Tracker.xlsx Module M14
 * (WF-01 sequential/parallel evaluation logic).
 */
class ApprovalWorkflowEngineTest {

    private ApprovalStepInstance step(int sequence, String role, DecisionStatus decision) {
        ApprovalStepInstance s = new ApprovalStepInstance();
        s.setStepInstanceId(sequence + "-" + role);
        s.setSequenceNumber(sequence);
        s.setApproverRole(role);
        s.setDecision(decision);
        return s;
    }

    // TC-0175/0176 (WF-01, Positive): a purely sequential 3-step chain, all pending initially.
    @Test
    void currentActionableSequence_forFreshSequentialChain_isTheFirstStep() {
        List<ApprovalStepInstance> steps = List.of(
                step(1, "COSTING_MANAGER", DecisionStatus.PENDING),
                step(2, "FINANCE_CONTROLLER", DecisionStatus.PENDING),
                step(3, "PURCHASING_MANAGER", DecisionStatus.PENDING));

        assertThat(ApprovalWorkflowEngine.currentActionableSequence(steps)).isEqualTo(1);
        assertThat(ApprovalWorkflowEngine.determineOutcome(steps)).isEqualTo(WorkflowInstanceStatus.IN_PROGRESS);
    }

    @Test
    void currentActionableSequence_afterFirstStepApproved_advancesToSecond() {
        List<ApprovalStepInstance> steps = List.of(
                step(1, "COSTING_MANAGER", DecisionStatus.APPROVED),
                step(2, "FINANCE_CONTROLLER", DecisionStatus.PENDING),
                step(3, "PURCHASING_MANAGER", DecisionStatus.PENDING));

        assertThat(ApprovalWorkflowEngine.currentActionableSequence(steps)).isEqualTo(2);
    }

    // WF-01 (Positive, parallel): two approvers share sequence 1 - both must approve before advancing.
    @Test
    void currentActionableSequence_withParallelStepsPartiallyApproved_staysAtTheParallelSequence() {
        List<ApprovalStepInstance> steps = List.of(
                step(1, "COSTING_MANAGER", DecisionStatus.APPROVED),
                step(1, "FINANCE_CONTROLLER", DecisionStatus.PENDING), // sibling in the same parallel group
                step(2, "PURCHASING_MANAGER", DecisionStatus.PENDING));

        assertThat(ApprovalWorkflowEngine.currentActionableSequence(steps)).isEqualTo(1);
        assertThat(ApprovalWorkflowEngine.currentActionableSteps(steps)).hasSize(1);
        assertThat(ApprovalWorkflowEngine.currentActionableSteps(steps).get(0).getApproverRole())
                .isEqualTo("FINANCE_CONTROLLER");
    }

    @Test
    void currentActionableSequence_withAllParallelStepsApproved_advancesPastTheGroup() {
        List<ApprovalStepInstance> steps = List.of(
                step(1, "COSTING_MANAGER", DecisionStatus.APPROVED),
                step(1, "FINANCE_CONTROLLER", DecisionStatus.APPROVED),
                step(2, "PURCHASING_MANAGER", DecisionStatus.PENDING));

        assertThat(ApprovalWorkflowEngine.currentActionableSequence(steps)).isEqualTo(2);
    }

    // WF-01 (Negative/Boundary): a single rejection anywhere fails the whole instance immediately.
    @Test
    void determineOutcome_withAnyStepRejected_isRejectedRegardlessOfOtherSteps() {
        List<ApprovalStepInstance> steps = List.of(
                step(1, "COSTING_MANAGER", DecisionStatus.APPROVED),
                step(2, "FINANCE_CONTROLLER", DecisionStatus.REJECTED),
                step(3, "PURCHASING_MANAGER", DecisionStatus.PENDING));

        assertThat(ApprovalWorkflowEngine.determineOutcome(steps)).isEqualTo(WorkflowInstanceStatus.REJECTED);
        assertThat(ApprovalWorkflowEngine.currentActionableSequence(steps)).isEqualTo(-1);
        assertThat(ApprovalWorkflowEngine.currentActionableSteps(steps)).isEmpty();
    }

    @Test
    void determineOutcome_withAStepReturned_isRejected() {
        List<ApprovalStepInstance> steps = List.of(
                step(1, "COSTING_MANAGER", DecisionStatus.RETURNED));

        assertThat(ApprovalWorkflowEngine.determineOutcome(steps)).isEqualTo(WorkflowInstanceStatus.REJECTED);
    }

    // TC-0187/0188 (WF-07-adjacent, Positive): every step approved -> instance approved.
    @Test
    void determineOutcome_withEveryStepApproved_isApproved() {
        List<ApprovalStepInstance> steps = List.of(
                step(1, "COSTING_MANAGER", DecisionStatus.APPROVED),
                step(2, "FINANCE_CONTROLLER", DecisionStatus.APPROVED));

        assertThat(ApprovalWorkflowEngine.determineOutcome(steps)).isEqualTo(WorkflowInstanceStatus.APPROVED);
        assertThat(ApprovalWorkflowEngine.currentActionableSequence(steps)).isEqualTo(-1);
    }
}
