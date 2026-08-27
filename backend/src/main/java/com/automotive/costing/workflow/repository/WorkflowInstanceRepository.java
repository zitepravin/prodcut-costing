package com.automotive.costing.workflow.repository;

import com.automotive.costing.workflow.entity.WorkflowInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowInstanceRepository extends JpaRepository<WorkflowInstance, String> {

    Optional<WorkflowInstance> findByWorkflowInstanceId(String workflowInstanceId);

    Optional<WorkflowInstance> findByCustomerIdAndWorkflowInstanceId(String customerId, String workflowInstanceId);

    List<WorkflowInstance> findByCustomerIdAndEntityTypeAndEntityIdOrderBySubmittedAtDesc(
            String customerId, String entityType, String entityId);


    List<WorkflowInstance> findByEntityTypeAndEntityIdOrderBySubmittedAtDesc(String entityType, String entityId);

    /** WF-06: "prevent a cost record from being used as an official baseline... until all required approval steps are complete." */
    boolean existsByEntityTypeAndEntityIdAndStatus(
            String entityType, String entityId, com.automotive.costing.workflow.entity.WorkflowInstanceStatus status);
}
