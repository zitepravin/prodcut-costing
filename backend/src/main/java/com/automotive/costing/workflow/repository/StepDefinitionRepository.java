package com.automotive.costing.workflow.repository;

import com.automotive.costing.workflow.entity.StepDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StepDefinitionRepository extends JpaRepository<StepDefinition, String> {

    List<StepDefinition> findByWorkflowDefinitionIdOrderBySequenceNumberAsc(String workflowDefinitionId);
}
