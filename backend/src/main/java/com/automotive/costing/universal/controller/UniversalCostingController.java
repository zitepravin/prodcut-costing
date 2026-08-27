package com.automotive.costing.universal.controller;

import com.automotive.costing.platform.access.RequiresModule;
import com.automotive.costing.universal.dto.UniversalCostingDtos.*;
import com.automotive.costing.universal.service.NormDefinitionService;
import com.automotive.costing.universal.service.SupplierSobService;
import com.automotive.costing.universal.service.UniversalCostingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Universal Costing API — module ON is necessary but not sufficient; each action is role-gated. */
@RestController
@RequestMapping("/api/v1/universal-costing")
@RequiredArgsConstructor
@RequiresModule("PRODUCT_COSTING")
public class UniversalCostingController {

    private final UniversalCostingService service;
    private final NormDefinitionService normService;
    private final SupplierSobService sobService;

    @GetMapping("/cost-objects")
    @PreAuthorize("hasAnyRole('VIEWER','COSTING_ENGINEER','COSTING_MANAGER','FINANCE_CONTROLLER',"
            + "'PURCHASING_BUYER','PROGRAM_COST_MANAGER','CUSTOMER_ADMIN','SYSTEM_ADMINISTRATOR','ADMIN')")
    public List<CostObjectResponse> listCostObjects() { return service.listCostObjects(); }

    @GetMapping("/cost-objects/{id}")
    @PreAuthorize("hasAnyRole('VIEWER','COSTING_ENGINEER','COSTING_MANAGER','FINANCE_CONTROLLER',"
            + "'PURCHASING_BUYER','PROGRAM_COST_MANAGER','CUSTOMER_ADMIN','SYSTEM_ADMINISTRATOR','ADMIN')")
    public CostObjectResponse getCostObject(@PathVariable("id") String id) { return service.getCostObject(id); }

    @GetMapping("/drivers")
    @PreAuthorize("hasAnyRole('VIEWER','COSTING_ENGINEER','COSTING_MANAGER','FINANCE_CONTROLLER',"
            + "'PROGRAM_COST_MANAGER','CUSTOMER_ADMIN','SYSTEM_ADMINISTRATOR','ADMIN')")
    public List<CostDriverResponse> listDrivers() { return service.listDrivers(); }

    @GetMapping("/pools")
    @PreAuthorize("hasAnyRole('VIEWER','COSTING_ENGINEER','COSTING_MANAGER','FINANCE_CONTROLLER',"
            + "'PROGRAM_COST_MANAGER','CUSTOMER_ADMIN','SYSTEM_ADMINISTRATOR','ADMIN')")
    public List<CostPoolResponse> listPools() { return service.listPools(); }

    @GetMapping("/calculations")
    @PreAuthorize("hasAnyRole('VIEWER','COSTING_ENGINEER','COSTING_MANAGER','FINANCE_CONTROLLER',"
            + "'PROGRAM_COST_MANAGER','CUSTOMER_ADMIN','SYSTEM_ADMINISTRATOR','ADMIN')")
    public List<CalculateResponse> listCalculations(@RequestParam(value="costObjectId",required=false) String costObjectId) {
        return service.listCalculations(costObjectId);
    }

    @GetMapping("/industry-profiles")
    @PreAuthorize("hasAnyRole('VIEWER','COSTING_ENGINEER','COSTING_MANAGER','FINANCE_CONTROLLER',"
            + "'PROGRAM_COST_MANAGER','CUSTOMER_ADMIN','SYSTEM_ADMINISTRATOR','ADMIN')")
    public List<IndustryProfileResponse> listIndustryProfiles() { return service.listIndustryProfiles(); }

    @GetMapping("/norms")
    @PreAuthorize("hasAnyRole('VIEWER','COSTING_ENGINEER','COSTING_MANAGER','FINANCE_CONTROLLER',"
            + "'PROGRAM_COST_MANAGER','CUSTOMER_ADMIN','SYSTEM_ADMINISTRATOR','ADMIN')")
    public List<NormResponse> listNorms() { return normService.list(); }

    @GetMapping("/sob")
    @PreAuthorize("hasAnyRole('VIEWER','COSTING_ENGINEER','COSTING_MANAGER','FINANCE_CONTROLLER',"
            + "'PROGRAM_COST_MANAGER','CUSTOMER_ADMIN','SYSTEM_ADMINISTRATOR','ADMIN')")
    public List<SobResponse> listSob() { return sobService.list(); }

    @PostMapping("/cost-objects")
    @PreAuthorize("hasAnyRole('COSTING_ENGINEER','COSTING_MANAGER','SYSTEM_ADMINISTRATOR','ADMIN')")
    public ResponseEntity<CostObjectResponse> createCostObject(@RequestBody CostObjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createCostObject(request));
    }

    @PostMapping("/drivers")
    @PreAuthorize("hasAnyRole('COSTING_ENGINEER','COSTING_MANAGER','SYSTEM_ADMINISTRATOR','ADMIN')")
    public ResponseEntity<CostDriverResponse> createDriver(@RequestBody CostDriverRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createDriver(request));
    }

    @PostMapping("/pools")
    @PreAuthorize("hasAnyRole('COSTING_ENGINEER','COSTING_MANAGER','SYSTEM_ADMINISTRATOR','ADMIN')")
    public ResponseEntity<CostPoolResponse> createPool(@RequestBody CostPoolRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createPool(request));
    }

    @PostMapping("/calculations")
    @PreAuthorize("hasAnyRole('COSTING_ENGINEER','COSTING_MANAGER','SYSTEM_ADMINISTRATOR','ADMIN')")
    public ResponseEntity<CalculateResponse> calculate(@RequestBody CalculateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.calculate(request));
    }

    @PostMapping("/norms")
    @PreAuthorize("hasAnyRole('COSTING_ENGINEER','COSTING_MANAGER','SYSTEM_ADMINISTRATOR','ADMIN')")
    public ResponseEntity<NormResponse> createNorm(@RequestBody NormRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(normService.create(request));
    }

    @PostMapping("/norms/{id}/versions")
    @PreAuthorize("hasAnyRole('COSTING_ENGINEER','COSTING_MANAGER','SYSTEM_ADMINISTRATOR','ADMIN')")
    public ResponseEntity<NormResponse> createNormVersion(@PathVariable("id") String id, @RequestBody NormRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(normService.createNewVersion(id, request));
    }

    @PostMapping("/sob")
    @PreAuthorize("hasAnyRole('COSTING_ENGINEER','COSTING_MANAGER','SYSTEM_ADMINISTRATOR','ADMIN')")
    public ResponseEntity<SobResponse> createSob(
            @RequestBody SobRequest request,
            @RequestParam(value = "allowPartial", required = false, defaultValue = "false") boolean allowPartial) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sobService.create(request, allowPartial));
    }

    @PostMapping("/calculations/{id}/lifecycle")
    @PreAuthorize("hasAnyRole('COSTING_ENGINEER','COSTING_MANAGER','FINANCE_CONTROLLER',"
            + "'SYSTEM_ADMINISTRATOR','ADMIN')")
    public CalculateResponse transition(@PathVariable("id") String id, @RequestBody LifecycleTransitionRequest request) {
        return service.transition(id, request);
    }

    @PostMapping("/norms/{id}/approve")
    @PreAuthorize("hasAnyRole('COSTING_MANAGER','FINANCE_CONTROLLER','SYSTEM_ADMINISTRATOR','ADMIN')")
    public NormResponse approveNorm(@PathVariable("id") String id,
            @RequestParam(value = "actorUserId", required = false) String actorUserId) {
        return normService.approve(id, actorUserId);
    }
}
