package com.automotive.costing.fta.controller;

import com.automotive.costing.fta.dto.FtaDtos.*;
import com.automotive.costing.fta.entity.TradeAgreement;
import com.automotive.costing.fta.entity.ProofOfOrigin;
import com.automotive.costing.fta.service.FtaAnalysisService;
import com.automotive.costing.fta.service.FtaDataImportService;
import com.automotive.costing.fta.service.ProofOfOriginService;
import com.automotive.costing.fta.service.FtaDataImportService.AgreementImportRow;
import com.automotive.costing.fta.service.FtaDataImportService.ConcessionImportRow;
import com.automotive.costing.fta.service.FtaDataImportService.ImportResult;
import com.automotive.costing.fta.service.ProofOfOriginService.ProofRequest;
import com.automotive.costing.platform.access.RequiresFeature;
import com.automotive.costing.platform.access.RequiresModule;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** FTA API — feature ON is necessary but not sufficient; each action is role-gated. */
@RestController
@RequestMapping("/api/v1/fta")
@RequiredArgsConstructor
@RequiresModule("PRODUCT_COSTING")
@RequiresFeature("FTA_TRADE_AGREEMENT")
public class FtaController {

  private final FtaAnalysisService service;
  private final ProofOfOriginService proofService;
  private final FtaDataImportService importService;

  @GetMapping("/analysis/{id}")
  @PreAuthorize("hasAnyRole('VIEWER','COSTING_ENGINEER','COSTING_MANAGER','FINANCE_CONTROLLER',"
      + "'PURCHASING_BUYER','SOURCING_LEAD','PROGRAM_COST_MANAGER','CUSTOMER_ADMIN',"
      + "'SYSTEM_ADMINISTRATOR','ADMIN')")
  public AnalyzeResponse get(@PathVariable String id) { return service.get(id); }

  @GetMapping("/agreements")
  @PreAuthorize("hasAnyRole('VIEWER','COSTING_ENGINEER','COSTING_MANAGER','FINANCE_CONTROLLER',"
      + "'PURCHASING_BUYER','SOURCING_LEAD','PROGRAM_COST_MANAGER','CUSTOMER_ADMIN',"
      + "'SYSTEM_ADMINISTRATOR','ADMIN')")
  public List<TradeAgreement> agreements() { return service.listAgreements(); }

  @GetMapping("/proof-of-origin/{id}")
  @PreAuthorize("hasAnyRole('VIEWER','COSTING_ENGINEER','COSTING_MANAGER','FINANCE_CONTROLLER',"
      + "'PURCHASING_BUYER','SOURCING_LEAD','PROGRAM_COST_MANAGER','CUSTOMER_ADMIN',"
      + "'SYSTEM_ADMINISTRATOR','ADMIN')")
  public ProofOfOrigin getProof(@PathVariable String id) { return proofService.get(id); }

  @PostMapping("/analyze")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('COSTING_ENGINEER','COSTING_MANAGER','SOURCING_LEAD',"
      + "'SYSTEM_ADMINISTRATOR','ADMIN')")
  public AnalyzeResponse analyze(@RequestBody AnalyzeRequest request) { return service.analyze(request); }

  @PostMapping("/proof-of-origin")
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('COSTING_ENGINEER','COSTING_MANAGER','SOURCING_LEAD',"
      + "'SYSTEM_ADMINISTRATOR','ADMIN')")
  public ProofOfOrigin createProof(@RequestBody ProofRequest body) { return proofService.create(body); }

  @PostMapping("/analysis/{id}/submit")
  @PreAuthorize("hasAnyRole('COSTING_ENGINEER','COSTING_MANAGER','SYSTEM_ADMINISTRATOR','ADMIN')")
  public AnalyzeResponse submit(@PathVariable String id) { return service.transition(id, "SUBMIT", null); }

  @PostMapping("/analysis/{id}/approve")
  @PreAuthorize("hasAnyRole('COSTING_MANAGER','FINANCE_CONTROLLER','SYSTEM_ADMINISTRATOR','ADMIN')")
  public AnalyzeResponse approve(@PathVariable String id, @RequestBody(required = false) Map<String, String> body) {
    return service.transition(id, "APPROVE", body != null ? body.get("actorUserId") : null);
  }

  @PostMapping("/analysis/{id}/lock")
  @PreAuthorize("hasAnyRole('COSTING_MANAGER','FINANCE_CONTROLLER','SYSTEM_ADMINISTRATOR','ADMIN')")
  public AnalyzeResponse lock(@PathVariable String id) { return service.transition(id, "LOCK", null); }

  @PostMapping("/proof-of-origin/{id}/{action}")
  @PreAuthorize("hasAnyRole('COSTING_ENGINEER','COSTING_MANAGER','FINANCE_CONTROLLER',"
      + "'SYSTEM_ADMINISTRATOR','ADMIN')")
  public ProofOfOrigin proofAction(@PathVariable String id, @PathVariable String action) {
    return proofService.transition(id, action);
  }

  @PostMapping("/import/agreements")
  @PreAuthorize("hasAnyRole('COSTING_MANAGER','SYSTEM_ADMINISTRATOR','ADMIN')")
  public ImportResult importAgreements(@RequestBody List<AgreementImportRow> rows) {
    return importService.importAgreements(rows);
  }

  @PostMapping("/import/concessions/{agreementId}")
  @PreAuthorize("hasAnyRole('COSTING_MANAGER','SYSTEM_ADMINISTRATOR','ADMIN')")
  public ImportResult importConcessions(@PathVariable String agreementId, @RequestBody List<ConcessionImportRow> rows) {
    return importService.importConcessions(agreementId, rows);
  }
}
