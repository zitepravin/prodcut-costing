package com.automotive.costing.configuration.service;

import com.automotive.costing.configuration.dto.ConfigurationDtos.EffectiveConfiguration;
import com.automotive.costing.configuration.dto.ConfigurationDtos.PackResponse;
import com.automotive.costing.workflow.entity.WorkflowDefinition;
import com.automotive.costing.workflow.repository.WorkflowDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * P3-07 Configuration Center aggregate read model.
 */
@Service
@RequiredArgsConstructor
public class ConfigurationFacadeService {

    private final CostingDefaultsService costingDefaultsService;
    private final QuestionnairePackService questionnairePackService;
    private final WorkflowDefinitionRepository workflowDefinitionRepository;

    @Transactional(readOnly = true)
    public EffectiveConfiguration effective(String customerId) {
        List<PackResponse> activePacks = questionnairePackService.list().stream()
                .filter(p -> "ACTIVE".equals(p.status()))
                .toList();
        return new EffectiveConfiguration(
                ModulePackCatalog.all(),
                costingDefaultsService.get(customerId),
                activePacks,
                List.of(
                        "Module packs are applied via Platform Customer entitlements (not auto-applied here).",
                        "Scorecard weights: see /api/v1/vendor-selection/scorecards/templates.",
                        "Workflow definitions: see /api/v1/workflow/definitions.",
                        "No AI auto-approve. FX missing = NOT_AVAILABLE. PR RELEASE incomplete = BLOCK."
                )
        );
    }

    @Transactional(readOnly = true)
    public List<WorkflowDefinition> listWorkflowDefinitions() {
        return workflowDefinitionRepository.findAll();
    }
}
