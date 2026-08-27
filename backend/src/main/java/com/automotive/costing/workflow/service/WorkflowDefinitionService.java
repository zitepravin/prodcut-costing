package com.automotive.costing.workflow.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.workflow.dto.WorkflowDefinitionDtos.*;
import com.automotive.costing.workflow.entity.StepDefinition;
import com.automotive.costing.workflow.entity.WorkflowDefinition;
import com.automotive.costing.workflow.repository.StepDefinitionRepository;
import com.automotive.costing.workflow.repository.WorkflowDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** WF-01/WF-02: configuration of approval workflow templates and their routing rules. */
@Service
@RequiredArgsConstructor
public class WorkflowDefinitionService {

    private static final String ENTITY_TYPE = "WorkflowDefinition";

    private final WorkflowDefinitionRepository definitionRepository;
    private final StepDefinitionRepository stepDefinitionRepository;
    private final AuditService auditService;

    @Transactional
    public Response create(CreateRequest request) {
        WorkflowDefinition definition = new WorkflowDefinition();
        definition.setWorkflowDefinitionId(UUID.randomUUID().toString());
        definition.setName(request.name());
        definition.setEntityType(request.entityType());
        definition.setActive(true);
        WorkflowDefinition savedDefinition = definitionRepository.save(definition);

        List<StepDefinition> steps = request.steps().stream()
                .map(s -> toStepEntity(savedDefinition.getWorkflowDefinitionId(), s))
                .toList();
        List<StepDefinition> savedSteps = stepDefinitionRepository.saveAll(steps);

        auditService.recordCreate(ENTITY_TYPE, savedDefinition.getWorkflowDefinitionId(),
                "workflow definition created with " + savedSteps.size() + " step(s)");
        return toResponse(savedDefinition, savedSteps);
    }

    @Transactional(readOnly = true)
    public Response getById(String workflowDefinitionId) {
        WorkflowDefinition definition = definitionRepository.findByWorkflowDefinitionId(workflowDefinitionId)
                .orElseThrow(() -> ResourceNotFoundException.of("WorkflowDefinition", workflowDefinitionId));
        List<StepDefinition> steps = stepDefinitionRepository
                .findByWorkflowDefinitionIdOrderBySequenceNumberAsc(workflowDefinitionId);
        return toResponse(definition, steps);
    }

    /** WF-02: every active definition configured for a given entity type - routing resolution itself lives in ApprovalWorkflowService. */
    @Transactional(readOnly = true)
    public List<WorkflowDefinition> activeDefinitionsFor(String entityType) {
        return definitionRepository.findByEntityTypeAndActiveTrue(entityType);
    }

    @Transactional(readOnly = true)
    public List<StepDefinition> stepsFor(String workflowDefinitionId) {
        return stepDefinitionRepository.findByWorkflowDefinitionIdOrderBySequenceNumberAsc(workflowDefinitionId);
    }

    private StepDefinition toStepEntity(String workflowDefinitionId, StepDefinitionRequest r) {
        StepDefinition step = new StepDefinition();
        step.setStepDefinitionId(UUID.randomUUID().toString());
        step.setWorkflowDefinitionId(workflowDefinitionId);
        step.setSequenceNumber(r.sequenceNumber());
        step.setApproverRole(r.approverRole());
        step.setMinValue(r.minValue());
        step.setMaxValue(r.maxValue());
        step.setCommodityFamily(r.commodityFamily());
        step.setPlantCode(r.plantCode());
        step.setEscalationHours(r.escalationHours());
        return step;
    }

    private Response toResponse(WorkflowDefinition definition, List<StepDefinition> steps) {
        List<StepDefinitionResponse> stepResponses = steps.stream()
                .map(s -> new StepDefinitionResponse(s.getStepDefinitionId(), s.getSequenceNumber(),
                        s.getApproverRole(), s.getMinValue(), s.getMaxValue(), s.getCommodityFamily(),
                        s.getPlantCode(), s.getEscalationHours()))
                .toList();
        return new Response(definition.getWorkflowDefinitionId(), definition.getName(), definition.getEntityType(),
                definition.isActive(), stepResponses);
    }
}
