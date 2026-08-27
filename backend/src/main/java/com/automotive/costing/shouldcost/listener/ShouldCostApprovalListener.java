package com.automotive.costing.shouldcost.listener;

import com.automotive.costing.common.event.WorkflowApprovedEvent;
import com.automotive.costing.common.event.WorkflowRejectedEvent;
import com.automotive.costing.common.event.WorkflowSubmittedEvent;
import com.automotive.costing.shouldcost.service.ShouldCostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Reacts to every M14 workflow lifecycle event relevant to a {@code ShouldCostBuildUp}:
 * WF-06 ("prevent use as an official baseline until approved") on approval, and SRS
 * 4.14.5 ("a rejected cost record returns to 'Draft' status") on rejection - completing
 * both halves of the seam the M08 session left open (only a manual, admin-callable
 * {@code lock()} existed at the time, since M14 didn't exist yet).
 *
 * {@code @TransactionalEventListener(AFTER_COMMIT)}, mirroring
 * {@code MaterialRateRecalculationListener}'s reasoning: only act once the approval
 * decision that triggered this has actually committed.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ShouldCostApprovalListener {

    private static final String ENTITY_TYPE = "ShouldCostBuildUp";

    private final ShouldCostService shouldCostService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWorkflowSubmitted(WorkflowSubmittedEvent event) {
        if (!ENTITY_TYPE.equals(event.entityType())) {
            return;
        }
        log.info("WF-03: workflow {} submitted for {}/{} - marking SUBMITTED.",
                event.workflowInstanceId(), event.entityType(), event.entityId());
        shouldCostService.markSubmitted(event.entityId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWorkflowApproved(WorkflowApprovedEvent event) {
        if (!ENTITY_TYPE.equals(event.entityType())) {
            return;
        }
        log.info("WF-06: workflow {} approved for {}/{} - locking as the official baseline.",
                event.workflowInstanceId(), event.entityType(), event.entityId());
        shouldCostService.lock(event.entityId());
    }

    /** SRS 4.14.5: "a rejected cost record returns to 'Draft' status and must restart the workflow from the beginning after correction." */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWorkflowRejected(WorkflowRejectedEvent event) {
        if (!ENTITY_TYPE.equals(event.entityType())) {
            return;
        }
        log.info("SRS 4.14.5: workflow {} rejected for {}/{} - returning to Draft.",
                event.workflowInstanceId(), event.entityType(), event.entityId());
        shouldCostService.markRejectedReturnToDraft(event.entityId());
    }
}
