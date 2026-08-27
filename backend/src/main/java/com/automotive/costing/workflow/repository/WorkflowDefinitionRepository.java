package com.automotive.costing.workflow.repository;

import com.automotive.costing.workflow.entity.WorkflowDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition, String> {

    Optional<WorkflowDefinition> findByWorkflowDefinitionId(String workflowDefinitionId);

    List<WorkflowDefinition> findByEntityTypeAndActiveTrue(String entityType);
}
