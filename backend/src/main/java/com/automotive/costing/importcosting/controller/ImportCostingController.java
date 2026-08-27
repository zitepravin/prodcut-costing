package com.automotive.costing.importcosting.controller;

import com.automotive.costing.importcosting.dto.ImportCostingDtos.*;
import com.automotive.costing.importcosting.entity.ImportDutyRule;
import com.automotive.costing.importcosting.entity.ImportTaxRule;
import com.automotive.costing.importcosting.service.ImportConfigService;
import com.automotive.costing.importcosting.service.ImportConfigService.DutyRuleRequest;
import com.automotive.costing.importcosting.service.ImportConfigService.TaxRuleRequest;
import com.automotive.costing.importcosting.service.ImportCostingService;
import com.automotive.costing.platform.access.RequiresFeature;
import com.automotive.costing.platform.access.RequiresModule;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Import Costing API — feature ON is necessary but not sufficient; each action is role-gated. */
@RestController
@RequestMapping("/api/v1/import-costing")
@RequiredArgsConstructor
@RequiresModule("PRODUCT_COSTING")
@RequiresFeature("IMPORT_COSTING")
public class ImportCostingController {

  private final ImportCostingService service;
  private final ImportConfigService configService;

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('VIEWER','COSTING_ENGINEER','COSTING_MANAGER','FINANCE_CONTROLLER',"
      + "'PURCHASING_BUYER','SOURCING_LEAD','PROGRAM_COST_MANAGER','CUSTOMER_ADMIN',"
      + "'SYSTEM_ADMINISTRATOR','ADMIN')")
  public CalculationResponse get(@PathVariable String id) { return service.get(id); }

  @GetMapping("/{id}/components")
  @PreAuthorize("hasAnyRole('VIEWER','COSTING_ENGINEER','COSTING_MANAGER','FINANCE_CONTROLLER',"
      + "'PURCHASING_BUYER','SOURCING_LEAD','PROGRAM_COST_MANAGER','CUSTOMER_ADMIN',"
      + "'SYSTEM_ADMINISTRATOR','ADMIN')")
  public List<ComponentResponse> components(@PathVariable String id) { return service.components(id); }

  @PostMapping("/calculate")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('COSTING_ENGINEER','COSTING_MANAGER','SYSTEM_ADMINISTRATOR','ADMIN')")
  public CalculationResponse calculate(@RequestBody CalculateRequest request) { return service.calculate(request); }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAnyRole('COSTING_MANAGER','SYSTEM_ADMINISTRATOR','ADMIN')")
  public void delete(@PathVariable String id) { service.delete(id); }

  @PostMapping("/{id}/submit")
  @PreAuthorize("hasAnyRole('COSTING_ENGINEER','COSTING_MANAGER','SYSTEM_ADMINISTRATOR','ADMIN')")
  public CalculationResponse submit(@PathVariable String id, @RequestBody(required = false) LifecycleRequest body) {
    return service.transition(id, body != null ? body : new LifecycleRequest("SUBMIT", null));
  }

  @PostMapping("/{id}/approve")
  @PreAuthorize("hasAnyRole('COSTING_MANAGER','FINANCE_CONTROLLER','SYSTEM_ADMINISTRATOR','ADMIN')")
  public CalculationResponse approve(@PathVariable String id, @RequestBody(required = false) LifecycleRequest body) {
    return service.transition(id, new LifecycleRequest("APPROVE", body != null ? body.actorUserId() : null));
  }

  @PostMapping("/{id}/lock")
  @PreAuthorize("hasAnyRole('COSTING_MANAGER','FINANCE_CONTROLLER','SYSTEM_ADMINISTRATOR','ADMIN')")
  public CalculationResponse lock(@PathVariable String id, @RequestBody(required = false) LifecycleRequest body) {
    return service.transition(id, body != null ? body : new LifecycleRequest("LOCK", null));
  }

  @PostMapping("/config/duty-rules")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('COSTING_MANAGER','SYSTEM_ADMINISTRATOR','ADMIN')")
  public ImportDutyRule createDuty(@RequestBody DutyRuleRequest body) { return configService.createDuty(body); }

  @PostMapping("/config/duty-rules/{id}/approve")
  @PreAuthorize("hasAnyRole('COSTING_MANAGER','FINANCE_CONTROLLER','SYSTEM_ADMINISTRATOR','ADMIN')")
  public ImportDutyRule approveDuty(@PathVariable String id) { return configService.approveDuty(id); }

  @PostMapping("/config/tax-rules")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('COSTING_MANAGER','SYSTEM_ADMINISTRATOR','ADMIN')")
  public ImportTaxRule createTax(@RequestBody TaxRuleRequest body) { return configService.createTax(body); }

  @PostMapping("/config/tax-rules/{id}/approve")
  @PreAuthorize("hasAnyRole('COSTING_MANAGER','FINANCE_CONTROLLER','SYSTEM_ADMINISTRATOR','ADMIN')")
  public ImportTaxRule approveTax(@PathVariable String id) { return configService.approveTax(id); }
}
