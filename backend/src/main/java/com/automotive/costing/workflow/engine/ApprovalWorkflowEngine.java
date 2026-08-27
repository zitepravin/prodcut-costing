package com.automotive.costing.workflow.engine;

import com.automotive.costing.workflow.entity.ApprovalStepInstance;
import com.automotive.costing.workflow.entity.DecisionStatus;
import com.automotive.costing.workflow.entity.WorkflowInstanceStatus;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * WF-01: pure logic for evaluating a workflow instance's overall state from its step
 * instances - framework-independent and unit-tested in isolation, same rationale as
 * {@code MaterialCostEngine}/{@code ProcessCostEngine}/etc., even though this "engine"
 * evaluates workflow state rather than doing financial arithmetic.
 *
 * Sequential vs. parallel: steps sharing the same {@code sequenceNumber} form a parallel
 * group. The instance advances past a sequence number only once EVERY step in that group
 * has a non-PENDING decision. A single REJECTED or RETURNED decision anywhere,
 * regardless of sequence or parallel grouping, immediately fails the whole instance
 * (SRS 4.14.5: "a rejected cost record returns to 'Draft' status and must restart the
 * workflow from the beginning after correction") - this engine does not implement the
 * SRS's optional "minor-revision fast path" exception to that rule, which is flagged as
 * not built in NEXT_AGENT.md.
 */
public final class ApprovalWorkflowEngine {

    private ApprovalWorkflowEngine() {
    }

    /** Overall instance status given its current step instances. */
    public static WorkflowInstanceStatus determineOutcome(List<ApprovalStepInstance> steps) {
        if (steps.stream().anyMatch(s -> s.getDecision() == DecisionStatus.REJECTED
                || s.getDecision() == DecisionStatus.RETURNED)) {
            return WorkflowInstanceStatus.REJECTED;
        }
        if (steps.stream().allMatch(s -> s.getDecision() == DecisionStatus.APPROVED)) {
            return WorkflowInstanceStatus.APPROVED;
        }
        return WorkflowInstanceStatus.IN_PROGRESS;
    }

    /**
     * The sequence number an approver may currently act on - the lowest sequenceNumber
     * that still has at least one PENDING step, provided every EARLIER sequence number is
     * fully APPROVED. Returns -1 if the workflow is already fully resolved (approved or
     * rejected) - there is nothing actionable left.
     */
    public static int currentActionableSequence(List<ApprovalStepInstance> steps) {
        if (determineOutcome(steps) != WorkflowInstanceStatus.IN_PROGRESS) {
            return -1;
        }
        Set<Integer> sequences = steps.stream()
                .map(ApprovalStepInstance::getSequenceNumber)
                .collect(Collectors.toCollection(java.util.TreeSet::new));

        for (int seq : sequences) {
            List<ApprovalStepInstance> atSequence = steps.stream()
                    .filter(s -> s.getSequenceNumber() == seq)
                    .toList();
            boolean anyPending = atSequence.stream().anyMatch(s -> s.getDecision() == DecisionStatus.PENDING);
            if (anyPending) {
                return seq;
            }
            // this sequence is fully approved - continue checking the next one
        }
        return -1;
    }

    /** The specific step instances an approver may currently act on. */
    public static List<ApprovalStepInstance> currentActionableSteps(List<ApprovalStepInstance> steps) {
        int seq = currentActionableSequence(steps);
        if (seq == -1) {
            return List.of();
        }
        return steps.stream()
                .filter(s -> s.getSequenceNumber() == seq && s.getDecision() == DecisionStatus.PENDING)
                .sorted(Comparator.comparing(ApprovalStepInstance::getApproverRole))
                .toList();
    }
}
