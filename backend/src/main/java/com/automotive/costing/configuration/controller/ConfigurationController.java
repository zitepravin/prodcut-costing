package com.automotive.costing.configuration.controller;

import com.automotive.costing.configuration.dto.ConfigurationDtos.CostingDefaultsRequest;
import com.automotive.costing.configuration.dto.ConfigurationDtos.CostingDefaultsResponse;
import com.automotive.costing.configuration.dto.ConfigurationDtos.CreatePackRequest;
import com.automotive.costing.configuration.dto.ConfigurationDtos.EffectiveConfiguration;
import com.automotive.costing.configuration.dto.ConfigurationDtos.ModulePackInfo;
import com.automotive.costing.configuration.dto.ConfigurationDtos.PackResponse;
import com.automotive.costing.configuration.service.ConfigurationFacadeService;
import com.automotive.costing.configuration.service.CostingDefaultsService;
import com.automotive.costing.configuration.service.ModulePackCatalog;
import com.automotive.costing.configuration.service.QuestionnairePackService;
import com.automotive.costing.workflow.entity.WorkflowDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * P3-07 Configuration Center APIs (PLATFORM admin).
 * Does not auto-approve suppliers or post to SAP.
 */
@RestController
@RequestMapping("/api/v1/configuration")
@RequiredArgsConstructor
@Tag(name = "Configuration Center (P3-07)")
public class ConfigurationController {

    private final QuestionnairePackService questionnairePackService;
    private final CostingDefaultsService costingDefaultsService;
    private final ConfigurationFacadeService facadeService;

    @GetMapping("/effective")
    @Operation(summary = "Resolved configuration read model for a customer")
    public ResponseEntity<EffectiveConfiguration> effective(
            @RequestParam(required = false) String customerId) {
        return ResponseEntity.ok(facadeService.effective(customerId));
    }

    @GetMapping("/module-packs")
    @Operation(summary = "P3-07d commercial module pack catalog")
    public ResponseEntity<List<ModulePackInfo>> modulePacks() {
        return ResponseEntity.ok(ModulePackCatalog.all());
    }

    @GetMapping("/questionnaire-packs")
    @Operation(summary = "P3-07b list questionnaire packs")
    public ResponseEntity<List<PackResponse>> listPacks() {
        return ResponseEntity.ok(questionnairePackService.list());
    }

    @PostMapping("/questionnaire-packs")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ADMIN', 'SOURCING_LEAD', 'COSTING_MANAGER')")
    @Operation(summary = "Create DRAFT questionnaire pack")
    public ResponseEntity<PackResponse> createPack(@Valid @RequestBody CreatePackRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(questionnairePackService.create(request));
    }

    @PostMapping("/questionnaire-packs/{packId}/publish")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ADMIN', 'SOURCING_LEAD', 'COSTING_MANAGER')")
    @Operation(summary = "Publish pack (must include all active mandatory-gate questions)")
    public ResponseEntity<PackResponse> publishPack(@PathVariable String packId) {
        return ResponseEntity.ok(questionnairePackService.publish(packId));
    }

    @PostMapping("/questionnaire-packs/{packId}/retire")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ADMIN', 'SOURCING_LEAD', 'COSTING_MANAGER')")
    public ResponseEntity<PackResponse> retirePack(@PathVariable String packId) {
        return ResponseEntity.ok(questionnairePackService.retire(packId));
    }

    @GetMapping("/costing-defaults")
    @Operation(summary = "P3-07c get tenant costing defaults")
    public ResponseEntity<CostingDefaultsResponse> getDefaults(@RequestParam String customerId) {
        return ResponseEntity.ok(costingDefaultsService.get(customerId));
    }

    @PutMapping("/costing-defaults")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ADMIN', 'CUSTOMER_ADMIN', 'COSTING_MANAGER')")
    @Operation(summary = "Upsert tenant costing defaults (FX/release policies are system-fixed)")
    public ResponseEntity<CostingDefaultsResponse> putDefaults(
            @RequestParam String customerId,
            @RequestBody CostingDefaultsRequest request) {
        return ResponseEntity.ok(costingDefaultsService.upsert(customerId, request));
    }

    @GetMapping("/workflow-definitions")
    @Operation(summary = "P3-07e list workflow definitions (read model for config center)")
    public ResponseEntity<List<WorkflowDefinition>> workflows() {
        return ResponseEntity.ok(facadeService.listWorkflowDefinitions());
    }
}
