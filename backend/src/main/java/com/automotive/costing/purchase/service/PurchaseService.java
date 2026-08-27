package com.automotive.costing.purchase.service;

import com.automotive.costing.bom.entity.BomNode;
import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.common.event.WorkflowSubmittedEvent;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.currency.dto.CurrencyDtos.ConvertRequest;
import com.automotive.costing.currency.service.CurrencyService;
import com.automotive.costing.makevsbuy.entity.MakeVsBuyLeg;
import com.automotive.costing.makevsbuy.entity.MakeVsBuySimulation;
import com.automotive.costing.makevsbuy.repository.MakeVsBuyLegRepository;
import com.automotive.costing.makevsbuy.repository.MakeVsBuySimulationRepository;
import com.automotive.costing.notification.entity.NotificationSeverity;
import com.automotive.costing.notification.entity.NotificationType;
import com.automotive.costing.notification.service.NotificationService;
import com.automotive.costing.platform.user.entity.ScopeType;
import com.automotive.costing.platform.user.entity.User;
import com.automotive.costing.platform.user.repository.UserRepository;
import com.automotive.costing.platform.user.service.AccessScopeService;
import com.automotive.costing.purchase.dto.PurchaseDtos.CreateFromMvbRequest;
import com.automotive.costing.purchase.dto.PurchaseDtos.CreateStandalonePurchaseRequest;
import com.automotive.costing.platform.access.CustomerContext;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.platform.access.service.CentralAccessService;
import com.automotive.costing.purchase.dto.PurchaseDtos.DecideRequest;
import com.automotive.costing.purchase.dto.PurchaseDtos.LineInput;
import com.automotive.costing.purchase.dto.PurchaseDtos.LineResponse;
import com.automotive.costing.purchase.dto.PurchaseDtos.PurchaseExportPackage;
import com.automotive.costing.purchase.dto.PurchaseDtos.PurchaseRequestResponse;
import com.automotive.costing.purchase.dto.PurchaseDtos.ReleaseRequest;
import com.automotive.costing.purchase.entity.PurchaseRequest;
import com.automotive.costing.purchase.entity.PurchaseRequestLine;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import com.automotive.costing.purchase.repository.PurchaseRequestLineRepository;
import com.automotive.costing.purchase.repository.PurchaseRequestRepository;
import com.automotive.costing.rfq.entity.QuoteStatus;
import com.automotive.costing.rfq.entity.SupplierMaster;
import com.automotive.costing.rfq.entity.SupplierQuote;
import com.automotive.costing.rfq.entity.SupplierStatus;
import com.automotive.costing.rfq.repository.SupplierMasterRepository;
import com.automotive.costing.rfq.repository.SupplierQuoteRepository;
import com.automotive.costing.rfq.service.RfqService;
import com.automotive.costing.rfq.dto.RfqDtos.CreateRfqRequest;
import com.automotive.costing.rfq.dto.RfqDtos.AddRfqLineRequest;
import com.automotive.costing.rfq.dto.RfqDtos.RfqResponse;
import com.automotive.costing.vendorselection.entity.SupplierLifecycleStatus;
import com.automotive.costing.vendorselection.entity.SupplierQualificationCurrent;
import com.automotive.costing.vendorselection.entity.SupplierTcoCalculation;
import com.automotive.costing.vendorselection.repository.SupplierQualificationCurrentRepository;
import com.automotive.costing.vendorselection.repository.SupplierTcoCalculationRepository;
import com.automotive.costing.workflow.dto.ApprovalWorkflowDtos.SubmitRequest;
import com.automotive.costing.workflow.service.ApprovalWorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * P3-01 Purchase Request service (V177 MVP + V178 A/B/D + V179 C/E/G).
 * A — qualification lifecycle must be APPROVED (fail closed).
 * B — ACCEPTED quote must match BOM node and supplier.
 * C — TCO snapshot from complete VS TCO when available (never invent).
 * D — decidedBy / releasedBy from authenticated principal.
 * E — on SUBMIT: optional workflow instance + submitter notification.
 * Human decision only — never auto-approve / auto-release.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseService {

    public static final String NOT_AVAILABLE = "NOT_AVAILABLE";
    public static final String AVAILABLE = "AVAILABLE";
    public static final String OP_PURCHASE_LINE = "PURCHASE_REQUEST_LINE";
    public static final String ENTITY_TYPE = "PURCHASE_REQUEST";

    private final PurchaseRequestRepository requestRepository;
    private final TenantAccessGuard tenantAccessGuard;
    private final PurchaseRequestLineRepository lineRepository;
    private final BomNodeRepository bomNodeRepository;
    private final MakeVsBuySimulationRepository simulationRepository;
    private final MakeVsBuyLegRepository legRepository;
    private final SupplierQuoteRepository quoteRepository;
    private final SupplierMasterRepository supplierMasterRepository;
    private final SupplierQualificationCurrentRepository qualificationCurrentRepository;
    private final SupplierTcoCalculationRepository tcoCalculationRepository;
    private final AccessScopeService accessScopeService;
    private final CentralAccessService centralAccessService;
    private final CurrencyService currencyService;
    private final RfqService rfqService;
    private final ApprovalWorkflowService approvalWorkflowService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PurchaseRequestResponse create(String bomNodeId, CreateFromMvbRequest request) {
        BomNode node = requireNode(bomNodeId);
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, node.getProgramId());

        String simId = blankToNull(request.makeVsBuySimulationId());
        String override = blankToNull(request.manualOverrideReason());
        MakeVsBuySimulation sim = null;

        if (simId != null) {
            sim = simulationRepository.findById(simId)
                    .orElseThrow(() -> new ResourceNotFoundException("Make-vs-Buy simulation", simId));
            tenantAccessGuard.assertOwns(sim.getCustomerId(), "MakeVsBuySimulation", simId);
            if (!bomNodeId.equals(sim.getBomNodeId())) {
                throw new IllegalArgumentException("Simulation does not belong to this BOM node");
            }
            if (!"DECIDED".equals(sim.getStatus())) {
                throw new IllegalArgumentException("Make-vs-Buy must be DECIDED before creating a purchase request");
            }
            String dc = sim.getDecisionCode() == null ? "" : sim.getDecisionCode().toUpperCase(Locale.ROOT);
            if (!List.of("APPROVE_BUY", "APPROVE_MIXED").contains(dc)) {
                throw new IllegalArgumentException("Only APPROVE_BUY or APPROVE_MIXED decisions can seed a purchase request");
            }
        } else if (override == null) {
            throw new IllegalArgumentException(
                    "Either makeVsBuySimulationId or manualOverrideReason is required");
        }

        String reporting = blankToNull(request.reportingCurrency());
        if (reporting == null && sim != null) {
            reporting = blankToNull(sim.getReportingCurrency());
        }
        if (reporting == null) {
            reporting = "USD";
        }
        reporting = reporting.toUpperCase(Locale.ROOT);

        PurchaseRequest pr = new PurchaseRequest();
        pr.setPurchaseRequestId(UUID.randomUUID().toString());
        pr.setPrNumber(generatePrNumber());
        pr.setProgramId(node.getProgramId());
        pr.setBomNodeId(bomNodeId);
        pr.setMakeVsBuySimulationId(simId);
        pr.setStatus(PurchaseRequestStatus.DRAFT);
        pr.setReportingCurrency(reporting);
        pr.setManualOverrideReason(override);
        pr.setNotes(blankToNull(request.notes()));
        pr.setPlantCode(blankToNull(request.plantCode()));
        pr.setRequesterUserId(currentUserId());
        pr.setDepartmentCode(blankToNull(request.departmentCode()));
        pr.setBuyerUserId(blankToNull(request.buyerUserId()));
        pr.setRequiredDate(request.requiredDate());
        pr.setPriorityCode(blankToNull(request.priorityCode()));
        pr.setPurposeText(blankToNull(request.purposeText()));
        pr.setCustomerId(tenantAccessGuard.requireCustomerId());
        requestRepository.save(pr);

        List<LineInput> inputs = request.lines() == null ? List.of() : request.lines();
        if (inputs.isEmpty() && sim != null) {
            inputs = seedLinesFromMvb(sim);
        }
        if (inputs.isEmpty()) {
            throw new IllegalArgumentException("At least one purchase line is required");
        }

        int lineNo = 1;
        for (LineInput in : inputs) {
            createLine(pr, lineNo++, in, reporting, bomNodeId);
        }

        return toResponse(pr, List.of(
                "Purchase request " + pr.getPrNumber() + " created as DRAFT (Wave B).",
                "Submit requires plant, required date, and line material/qty/uom.",
                "Supplier may be set later after RFQ; APPROVED supplier required when supplier is present.",
                "Human submit/approve required."
        ));
    }

    @Transactional(readOnly = true)
    public PurchaseRequestResponse get(String id) {
        PurchaseRequest pr = requirePr(id);
        assertProgramAccessIfPresent(pr);
        return toResponse(pr, List.of());
    }


    /**
     * Purchase-native PR create — no BOM node / no Product Costing module required.
     * Enables PURCHASE-only tenants to run full PR → PO cycle.
     */
    @Transactional
    public PurchaseRequestResponse createStandalone(CreateStandalonePurchaseRequest request) {
        String customerId = CustomerContext.getCustomerId();
        if (customerId == null || customerId.isBlank()) {
            throw new BusinessRuleViolationException("X-Customer-Id is required", "ACCESS-07");
        }
        if (request == null || request.lines() == null || request.lines().isEmpty()) {
            throw new IllegalArgumentException("At least one purchase line is required");
        }
        String reporting = request.reportingCurrency() == null || request.reportingCurrency().isBlank()
                ? "USD" : request.reportingCurrency().trim().toUpperCase(Locale.ROOT);

        PurchaseRequest pr = new PurchaseRequest();
        pr.setPurchaseRequestId(UUID.randomUUID().toString());
        pr.setCustomerId(customerId);
        pr.setPrNumber(generateStandalonePrNumber(customerId));
        pr.setProgramId(null);
        pr.setBomNodeId(null);
        pr.setStatus(PurchaseRequestStatus.DRAFT);
        pr.setReportingCurrency(reporting);
        pr.setPlantCode(blankToNull(request.plantCode()));
        pr.setDepartmentCode(blankToNull(request.departmentCode()));
        pr.setBuyerUserId(blankToNull(request.buyerUserId()) != null
                ? blankToNull(request.buyerUserId()) : currentUserId());
        pr.setRequiredDate(request.requiredDate());
        pr.setPriorityCode(blankToNull(request.priorityCode()));
        pr.setPurposeText(blankToNull(request.purposeText()));
        pr.setRequesterUserId(currentUserId());
        // notes field if exists on entity - skip if not
        requestRepository.save(pr);

        int lineNo = 1;
        for (LineInput in : request.lines()) {
            createLine(pr, lineNo++, in, reporting, null);
        }
        return toResponse(pr, List.of(
                "Standalone purchase request " + pr.getPrNumber() + " created as DRAFT.",
                "No Product Costing BOM required (PURCHASE module native).",
                "Human submit/approve required."
        ));
    }

    @Transactional(readOnly = true)
    public List<PurchaseRequestResponse> listForCurrentCustomer() {
        String customerId = tenantAccessGuard.requireCustomerId();
        return requestRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(pr -> toResponse(pr, List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PurchaseRequestResponse> listByBomNode(String bomNodeId) {
        BomNode node = requireNode(bomNodeId);
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, node.getProgramId());
        String customerId = tenantAccessGuard.requireCustomerId();
        return requestRepository.findByCustomerIdAndBomNodeIdOrderByCreatedAtDesc(customerId, bomNodeId).stream()
                .map(pr -> toResponse(pr, List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PurchaseRequestResponse> listByProgram(String programId) {
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, programId);
        String customerId = tenantAccessGuard.requireCustomerId();
        return requestRepository.findByCustomerIdAndProgramIdOrderByCreatedAtDesc(customerId, programId).stream()
                .map(pr -> toResponse(pr, List.of()))
                .toList();
    }

    @Transactional
    public PurchaseRequestResponse submit(String id) {
        PurchaseRequest pr = requirePr(id);
        assertProgramAccessIfPresent(pr);
        PurchaseRequestStatus.assertTransition(pr.getStatus(), PurchaseRequestStatus.SUBMITTED);
        List<PurchaseRequestLine> lines = lineRepository.findByPurchaseRequestIdOrderByLineNoAsc(id);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Cannot submit without lines");
        }
        assertMandatoryForSubmit(pr, lines);
        for (PurchaseRequestLine line : lines) {
            if (line.getSupplierId() != null && !line.getSupplierId().isBlank()) {
                assertSupplierApprovedForPurchase(line.getSupplierId());
            }
        }
        pr.setStatus(PurchaseRequestStatus.SUBMITTED);
        requestRepository.save(pr);

        List<String> messages = new ArrayList<>();
        messages.add("Submitted. Awaiting human approval.");

        // Hardening E — optional formal workflow + submitter notification
        String workflowInstanceId = tryStartWorkflow(pr, lines);
        if (workflowInstanceId != null) {
            messages.add("Workflow instance started: " + workflowInstanceId);
            eventPublisher.publishEvent(new WorkflowSubmittedEvent(ENTITY_TYPE, id, workflowInstanceId));
        } else {
            messages.add("No PURCHASE_REQUEST workflow definition active — human decide API remains available.");
        }
        notifySubmitter(pr);

        return toResponse(pr, messages);
    }

    @Transactional
    public PurchaseRequestResponse decide(String id, DecideRequest request) {
        PurchaseRequest pr = requirePr(id);
        assertProgramAccessIfPresent(pr);
        String st = PurchaseRequestStatus.norm(pr.getStatus());
        if (!List.of(PurchaseRequestStatus.SUBMITTED, PurchaseRequestStatus.UNDER_REVIEW, "APPROVAL_PENDING").contains(st)) {
            throw new IllegalArgumentException("Decide only from SUBMITTED, UNDER_REVIEW, or APPROVAL_PENDING");
        }
        String code = request.decisionCode().trim().toUpperCase(Locale.ROOT);
        if (!List.of("APPROVE", "REJECT").contains(code)) {
            throw new IllegalArgumentException("decisionCode must be APPROVE or REJECT");
        }
        if (request.decisionReason() == null || request.decisionReason().isBlank()) {
            throw new IllegalArgumentException("decisionReason is required");
        }
        if ("APPROVE".equals(code)) {
            for (PurchaseRequestLine line : lineRepository.findByPurchaseRequestIdOrderByLineNoAsc(id)) {
                if (line.getSupplierId() != null && !line.getSupplierId().isBlank()) {
                    assertSupplierApprovedForPurchase(line.getSupplierId());
                }
            }
        }
        String next = "APPROVE".equals(code) ? PurchaseRequestStatus.APPROVED : PurchaseRequestStatus.REJECTED;
        PurchaseRequestStatus.assertTransition(
                st.equals("APPROVAL_PENDING") ? PurchaseRequestStatus.SUBMITTED : st, next);
        pr.setDecisionCode(code);
        pr.setDecisionReason(request.decisionReason().trim());
        pr.setDecidedAt(Instant.now());
        pr.setDecidedBy(currentUserId());
        pr.setStatus(next);
        requestRepository.save(pr);
        notifyDecision(pr, code);
        return toResponse(pr, List.of("Human decision recorded: " + code + " by " + nullToDash(pr.getDecidedBy())));
    }

    @Transactional
    public PurchaseRequestResponse release(String id, ReleaseRequest request) {
        PurchaseRequest pr = requirePr(id);
        assertProgramAccessIfPresent(pr);
        PurchaseRequestStatus.assertTransition(pr.getStatus(), PurchaseRequestStatus.RELEASED);
        List<PurchaseRequestLine> lines = lineRepository.findByPurchaseRequestIdOrderByLineNoAsc(id);
        for (PurchaseRequestLine line : lines) {
            if (NOT_AVAILABLE.equals(line.getDataStatus())) {
                throw new IllegalArgumentException(
                        "Cannot release: line " + line.getLineNo() + " has dataStatus NOT_AVAILABLE (missing price or FX)");
            }
            if (line.getSupplierId() != null && !line.getSupplierId().isBlank()) {
                assertSupplierApprovedForPurchase(line.getSupplierId());
            }
        }
        if (request != null && request.externalPoReference() != null && !request.externalPoReference().isBlank()) {
            pr.setExternalPoReference(request.externalPoReference().trim());
            pr.setPoLinkStatus("LINKED");
            pr.setPoLinkedSource("HUMAN_RELEASE");
            pr.setExternalPoSystem(pr.getExternalPoSystem() == null ? "SAP_MM" : pr.getExternalPoSystem());
            pr.setPoLinkedAt(java.time.Instant.now());
        }
        pr.setStatus(PurchaseRequestStatus.RELEASED);
        pr.setReleasedAt(Instant.now());
        pr.setReleasedBy(currentUserId());
        requestRepository.save(pr);
        notifyReleased(pr);
        return toResponse(pr, List.of("Released for ERP/CPI export by " + nullToDash(pr.getReleasedBy())
                + ". System does not post to SAP MM."));
    }

    @Transactional(readOnly = true)
    public PurchaseExportPackage exportPackage(String id) {
        PurchaseRequest pr = requirePr(id);
        assertProgramAccessIfPresent(pr);
        List<LineResponse> lines = lineRepository.findByPurchaseRequestIdOrderByLineNoAsc(id).stream()
                .map(this::toLineResponse)
                .toList();
        return new PurchaseExportPackage(
                pr.getPurchaseRequestId(),
                pr.getPrNumber(),
                pr.getStatus(),
                pr.getProgramId(),
                pr.getBomNodeId(),
                pr.getPlantCode(),
                pr.getMakeVsBuySimulationId(),
                pr.getLinkedRfqId(),
                pr.getReportingCurrency(),
                pr.getDecisionCode(),
                pr.getDecisionReason(),
                pr.getDecidedBy(),
                pr.getDecidedAt(),
                pr.getReleasedAt(),
                pr.getExternalPoReference(),
                lines,
                List.of(
                        "Read-only export for CPI/ERP process teams.",
                        "This endpoint does not post to SAP MM / ME21N.",
                        "Human approval is mandatory; AI must not approve or release.",
                        "Hardening A/B/C/D/E: APPROVED qualification, quote match, TCO snapshot, actor identity, submit notification."
                )
        );
    }

    /**
     * Hardening A: supplier qualification lifecycle must be APPROVED.
     * Fail closed if missing, BLOCKED, SUSPENDED, or any non-APPROVED status.
     */
    /**
     * Supplier qualification gate is enforced only when the tenant is entitled to VENDOR_SELECTION.
     * PURCHASE-only tenants may reference suppliers without VS lifecycle data.
     * When VS is entitled: APPROVED required; BLOCKED/SUSPENDED always rejected if lifecycle exists.
     */
    void assertSupplierApprovedForPurchase(String supplierId) {
        if (supplierId == null || supplierId.isBlank()) {
            throw new IllegalArgumentException("supplierId is required");
        }
        String customerId = CustomerContext.getCustomerId();
        boolean vsEntitled = false;
        if (customerId != null && !customerId.isBlank()) {
            try {
                centralAccessService.assertModuleEnabled(customerId, "VENDOR_SELECTION");
                vsEntitled = true;
            } catch (RuntimeException ignored) {
                vsEntitled = false;
            }
        }
        String tenantIdQual = customerId != null && !customerId.isBlank()
                ? customerId
                : tenantAccessGuard.requireCustomerId();
        SupplierQualificationCurrent current = qualificationCurrentRepository
                .findByCustomerIdAndSupplierId(tenantIdQual, supplierId).orElse(null);
        if (!vsEntitled) {
            // Purchase-only: no VS gate. If lifecycle exists and is BLOCKED/SUSPENDED, still fail closed.
            if (current != null && current.getLifecycleStatus() != null) {
                SupplierLifecycleStatus status = current.getLifecycleStatus();
                if (status == SupplierLifecycleStatus.BLOCKED || status == SupplierLifecycleStatus.SUSPENDED) {
                    throw new IllegalArgumentException(
                            "Supplier " + supplierId + " lifecycle is " + status.name()
                                    + " — cannot use on purchase request");
                }
            }
            return;
        }
        if (current == null || current.getLifecycleStatus() == null) {
            throw new IllegalArgumentException(
                    "Supplier " + supplierId + " has no qualification lifecycle — APPROVED required for purchase");
        }
        SupplierLifecycleStatus status = current.getLifecycleStatus();
        if (status == SupplierLifecycleStatus.BLOCKED || status == SupplierLifecycleStatus.SUSPENDED) {
            throw new IllegalArgumentException(
                    "Supplier " + supplierId + " lifecycle is " + status.name()
                            + " — cannot use on purchase request");
        }
        if (status != SupplierLifecycleStatus.APPROVED) {
            throw new IllegalArgumentException(
                    "Supplier " + supplierId + " lifecycle is " + status.name()
                            + " — APPROVED qualification required for purchase (hardening A)");
        }
    }

    private List<LineInput> seedLinesFromMvb(MakeVsBuySimulation sim) {
        List<MakeVsBuyLeg> legs = legRepository.findBySimulationIdOrderByLegSeqAsc(sim.getSimulationId());
        List<LineInput> out = new ArrayList<>();
        for (MakeVsBuyLeg leg : legs) {
            if (!"BUY".equalsIgnoreCase(leg.getStrategyType())) {
                continue;
            }
            if ("NOT_ELIGIBLE".equalsIgnoreCase(leg.getEligibilityStatus())) {
                continue;
            }
            if (leg.getSupplierId() == null || leg.getSupplierId().isBlank()) {
                continue;
            }
            out.add(new LineInput(
                    leg.getSupplierId(),
                    leg.getSourceRefId(),
                    null,
                    null,
                    BigDecimal.ONE,
                    "EA",
                    null,
                    null,
                    null,
                    null,
                    leg.getEligibilityNotes()
            ));
        }
        return out;
    }

    private void createLine(PurchaseRequest pr, int lineNo, LineInput in, String reportingCurrency, String bomNodeId) {
        if (in.quantity() == null || in.quantity().signum() <= 0) {
            throw new IllegalArgumentException("quantity must be positive on line " + lineNo);
        }

        String supplierId = blankToNull(in.supplierId());
        if (supplierId != null) {
            String tenantIdSup = tenantAccessGuard.requireCustomerId();
            SupplierMaster supplier = supplierMasterRepository.findByCustomerIdAndSupplierId(tenantIdSup, supplierId)
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier", supplierId));
            if (supplier.getStatus() == SupplierStatus.BLOCKED) {
                throw new IllegalArgumentException("Supplier is BLOCKED — cannot add to purchase request");
            }
            assertSupplierApprovedForPurchase(supplierId);
        }

        PurchaseRequestLine line = new PurchaseRequestLine();
        line.setLineId(UUID.randomUUID().toString());
        line.setPurchaseRequestId(pr.getPurchaseRequestId());
        line.setLineNo(lineNo);
        line.setMaterialCode(blankToNull(in.materialCode()));
        line.setMaterialDescription(blankToNull(in.materialDescription()));
        line.setSupplierId(supplierId);
        line.setQuantity(in.quantity());
        line.setUom(in.uom() == null || in.uom().isBlank() ? "EA" : in.uom().trim());
        line.setTargetPrice(in.targetPrice());
        line.setTargetCurrency(blankToNull(in.targetCurrency()));
        line.setCostCenterCode(blankToNull(in.costCenterCode()));
        line.setNeedByDate(in.needByDate());
        line.setLineNotes(blankToNull(in.lineNotes()));
        line.setTcoStatus(NOT_AVAILABLE);
        line.setDataStatus(NOT_AVAILABLE);

        String quoteId = blankToNull(in.supplierQuoteId());
        if (quoteId != null) {
            if (supplierId == null) {
                throw new IllegalArgumentException("supplierId required when supplierQuoteId is set on line " + lineNo);
            }
            applyAcceptedQuote(line, quoteId, bomNodeId, supplierId, pr.getManualOverrideReason());
            applyFx(line, reportingCurrency);
        }

        applyTcoSnapshot(line, supplierId, bomNodeId);

        lineRepository.save(line);
    }

    /**
     * Hardening C: snapshot latest complete SupplierTcoCalculation for supplier + BOM.
     * Missing or incomplete TCO remains NOT_AVAILABLE (never invent zero).
     */
    void applyTcoSnapshot(PurchaseRequestLine line, String supplierId, String bomNodeId) {
        line.setTcoStatus(NOT_AVAILABLE);
        line.setTcoUnit(null);
        if (supplierId == null || bomNodeId == null) {
            return;
        }
        List<SupplierTcoCalculation> candidates = tcoCalculationRepository
                .findBySupplierIdOrderByCalculatedAtDesc(supplierId);
        SupplierTcoCalculation best = candidates.stream()
                .filter(t -> bomNodeId.equals(t.getBomNodeId()))
                .filter(SupplierTcoCalculation::isDataComplete)
                .filter(t -> t.getTcoTotal() != null)
                .max(Comparator.comparing(SupplierTcoCalculation::getCalculatedAt))
                .orElse(null);
        if (best == null) {
            return;
        }
        BigDecimal unit = best.getTcoTotal();
        if (best.getAnnualVolume() != null && best.getAnnualVolume().signum() > 0) {
            unit = best.getTcoTotal().divide(best.getAnnualVolume(), 6, RoundingMode.HALF_UP);
        }
        line.setTcoUnit(unit);
        line.setTcoStatus(AVAILABLE);
    }

    /**
     * Hardening B: quote must exist, match BOM node and supplier; ACCEPTED unless manual override.
     */
    private void applyAcceptedQuote(
            PurchaseRequestLine line,
            String quoteId,
            String bomNodeId,
            String supplierId,
            String manualOverrideReason
    ) {
        String tenantIdQuote = tenantAccessGuard.requireCustomerId();
        SupplierQuote quote = quoteRepository.findByCustomerIdAndQuoteId(tenantIdQuote, quoteId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier quote", quoteId));

        if (quote.getBomNodeId() == null || !quote.getBomNodeId().equals(bomNodeId)) {
            throw new IllegalArgumentException(
                    "Quote " + quoteId + " bom_node_id does not match purchase request BOM node (hardening B)");
        }
        if (quote.getSupplierId() == null || !quote.getSupplierId().equals(supplierId)) {
            throw new IllegalArgumentException(
                    "Quote " + quoteId + " supplier_id does not match line supplier (hardening B)");
        }
        if (quote.getStatus() != QuoteStatus.ACCEPTED) {
            if (manualOverrideReason == null || manualOverrideReason.isBlank()) {
                throw new IllegalArgumentException(
                        "Quote " + quoteId + " is " + quote.getStatus()
                                + " (not ACCEPTED); provide manualOverrideReason to proceed");
            }
            line.setLineNotes(appendNote(line.getLineNotes(),
                    "Non-ACCEPTED quote used with manualOverrideReason; status=" + quote.getStatus()));
        }
        line.setSupplierQuoteId(quoteId);
        line.setUnitPrice(quote.getUnitPrice());
        line.setCurrency(quote.getCurrencyCode());
    }

    private void applyFx(PurchaseRequestLine line, String reportingCurrency) {
        if (line.getUnitPrice() == null || line.getCurrency() == null) {
            line.setDataStatus(NOT_AVAILABLE);
            return;
        }
        String from = line.getCurrency().toUpperCase(Locale.ROOT);
        String to = reportingCurrency.toUpperCase(Locale.ROOT);
        if (from.equals(to)) {
            line.setUnitPriceReporting(line.getUnitPrice());
            line.setFxRate(BigDecimal.ONE);
            line.setDataStatus(AVAILABLE);
            return;
        }
        try {
            var resp = currencyService.convert(new ConvertRequest(line.getUnitPrice(), from, to, null));
            line.setUnitPriceReporting(resp.convertedAmount());
            line.setFxRate(resp.rateUsed());
            line.setDataStatus(AVAILABLE);
        } catch (Exception ex) {
            line.setUnitPriceReporting(null);
            line.setFxRate(null);
            line.setDataStatus(NOT_AVAILABLE);
            line.setLineNotes(appendNote(line.getLineNotes(), "FX rate not available " + from + "→" + to));
        }
    }

    /**
     * Hardening E: try formal workflow when a PURCHASE_REQUEST definition exists.
     * Fail open (status still SUBMITTED) if no definition or routing mismatch.
     */
    private String tryStartWorkflow(PurchaseRequest pr, List<PurchaseRequestLine> lines) {
        String userId = currentUserId();
        if (userId == null) {
            return null;
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.debug("Submitter user {} not found — skip formal workflow", userId);
            return null;
        }
        BigDecimal value = BigDecimal.ZERO;
        for (PurchaseRequestLine line : lines) {
            if (line.getUnitPriceReporting() != null && line.getQuantity() != null) {
                value = value.add(line.getUnitPriceReporting().multiply(line.getQuantity()));
            }
        }
        try {
            var response = approvalWorkflowService.submit(
                    new SubmitRequest(ENTITY_TYPE, pr.getPurchaseRequestId(), value, null, null),
                    user);
            return response.workflowInstanceId();
        } catch (BusinessRuleViolationException ex) {
            log.info("PURCHASE_REQUEST workflow not started ({}): {}", ex.getRuleReference(), ex.getMessage());
            return null;
        } catch (Exception ex) {
            log.warn("PURCHASE_REQUEST workflow start failed: {}", ex.getMessage());
            return null;
        }
    }

    private void notifySubmitter(PurchaseRequest pr) {
        String userId = currentUserId();
        if (userId == null) {
            return;
        }
        try {
            notificationService.publish(
                    userId,
                    NotificationType.PURCHASE_REQUEST_SUBMITTED,
                    "Purchase request submitted",
                    "Purchase request " + displayPr(pr)
                            + " was submitted and awaits human approval.",
                    ENTITY_TYPE,
                    pr.getPurchaseRequestId(),
                    NotificationSeverity.INFO);
        } catch (Exception ex) {
            log.warn("Could not publish PURCHASE_REQUEST_SUBMITTED notification: {}", ex.getMessage());
        }
    }

    /**
     * After human APPROVE/REJECT: notify PR creator (in-app always; email if SMTP enabled).
     * Does not notify the deciding user when they are the only actor.
     */
    private void notifyDecision(PurchaseRequest pr, String decisionCode) {
        String recipient = resolveNotificationRecipient(pr);
        if (recipient == null) {
            log.warn("No recipient for PR decision notification on {}", pr.getPurchaseRequestId());
            return;
        }
        boolean approved = "APPROVE".equals(decisionCode);
        NotificationType type = approved
                ? NotificationType.PURCHASE_REQUEST_APPROVED
                : NotificationType.PURCHASE_REQUEST_REJECTED;
        String title = approved ? "Purchase request approved" : "Purchase request rejected";
        String body = "Purchase request " + displayPr(pr)
                + " was " + (approved ? "approved" : "rejected")
                + " by " + nullToDash(pr.getDecidedBy())
                + ". Reason: " + nullToDash(pr.getDecisionReason())
                + (approved ? " You may proceed to release for ERP export when ready." : " Revise and resubmit if appropriate.");
        NotificationSeverity severity = approved ? NotificationSeverity.INFO : NotificationSeverity.WARNING;
        try {
            notificationService.publish(
                    recipient,
                    type,
                    title,
                    body,
                    ENTITY_TYPE,
                    pr.getPurchaseRequestId(),
                    severity);
        } catch (Exception ex) {
            log.warn("Could not publish {} notification: {}", type, ex.getMessage());
        }
    }

    /**
     * After RELEASE: notify PR creator that export package is ready for CPI/ERP.
     * This is not an SAP PO-created email.
     */
    private void notifyReleased(PurchaseRequest pr) {
        String recipient = resolveNotificationRecipient(pr);
        if (recipient == null) {
            return;
        }
        String body = "Purchase request " + displayPr(pr)
                + " was released for ERP/CPI export by " + nullToDash(pr.getReleasedBy())
                + ". Export package is available. This system does not post to SAP MM.";
        try {
            notificationService.publish(
                    recipient,
                    NotificationType.PURCHASE_REQUEST_RELEASED,
                    "Purchase request released for ERP",
                    body,
                    ENTITY_TYPE,
                    pr.getPurchaseRequestId(),
                    NotificationSeverity.INFO);
        } catch (Exception ex) {
            log.warn("Could not publish PURCHASE_REQUEST_RELEASED notification: {}", ex.getMessage());
        }
    }

    private String resolveNotificationRecipient(PurchaseRequest pr) {
        if (pr.getCreatedBy() != null && !pr.getCreatedBy().isBlank()) {
            return pr.getCreatedBy();
        }
        return null;
    }

    private static String displayPr(PurchaseRequest pr) {
        if (pr.getPrNumber() != null && !pr.getPrNumber().isBlank()) {
            return pr.getPrNumber() + " (" + pr.getPurchaseRequestId() + ")";
        }
        return pr.getPurchaseRequestId();
    }

    private PurchaseRequestResponse toResponse(PurchaseRequest pr, List<String> messages) {
        List<LineResponse> lines = lineRepository
                .findByPurchaseRequestIdOrderByLineNoAsc(pr.getPurchaseRequestId()).stream()
                .map(this::toLineResponse)
                .toList();
        return new PurchaseRequestResponse(
                pr.getPurchaseRequestId(),
                pr.getPrNumber(),
                pr.getProgramId(),
                pr.getBomNodeId(),
                pr.getMakeVsBuySimulationId(),
                pr.getLinkedRfqId(),
                pr.getStatus(),
                pr.getPlantCode(),
                pr.getRequesterUserId(),
                pr.getDepartmentCode(),
                pr.getBuyerUserId(),
                pr.getRequiredDate(),
                pr.getPriorityCode(),
                pr.getPurposeText(),
                pr.getReportingCurrency(),
                pr.getManualOverrideReason(),
                pr.getDecisionCode(),
                pr.getDecisionReason(),
                pr.getDecidedBy(),
                pr.getDecidedAt(),
                pr.getReleasedAt(),
                pr.getExternalPoReference(),
                pr.getPoLinkStatus() == null ? "NOT_LINKED" : pr.getPoLinkStatus(),
                pr.getExternalPoSystem(),
                pr.getPoLinkedAt(),
                pr.getPoLinkedSource(),
                pr.getNotes(),
                lines,
                messages
        );
    }

    private LineResponse toLineResponse(PurchaseRequestLine line) {
        return new LineResponse(
                line.getLineId(),
                line.getLineNo(),
                line.getMaterialCode(),
                line.getMaterialDescription(),
                line.getSupplierId(),
                line.getSupplierQuoteId(),
                line.getQuantity(),
                line.getUom(),
                line.getTargetPrice(),
                line.getTargetCurrency(),
                line.getCostCenterCode(),
                line.getUnitPrice(),
                line.getCurrency(),
                line.getUnitPriceReporting(),
                line.getFxRate(),
                line.getDataStatus(),
                line.getTcoUnit(),
                line.getTcoStatus(),
                line.getNeedByDate(),
                line.getLineNotes()
        );
    }

    private void assertMandatoryForSubmit(PurchaseRequest pr, List<PurchaseRequestLine> lines) {
        if (pr.getPlantCode() == null || pr.getPlantCode().isBlank()) {
            throw new IllegalArgumentException("plantCode is required before submit");
        }
        if (pr.getRequiredDate() == null) {
            throw new IllegalArgumentException("requiredDate is required before submit");
        }
        for (PurchaseRequestLine line : lines) {
            if (line.getQuantity() == null || line.getQuantity().signum() <= 0) {
                throw new IllegalArgumentException("Line " + line.getLineNo() + ": quantity is required");
            }
            if (line.getUom() == null || line.getUom().isBlank()) {
                throw new IllegalArgumentException("Line " + line.getLineNo() + ": uom is required");
            }
            if (line.getMaterialCode() == null || line.getMaterialCode().isBlank()) {
                throw new IllegalArgumentException("Line " + line.getLineNo() + ": materialCode is required before submit");
            }
        }
    }

    private String generatePrNumber() {
        String day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return "PR-" + day + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Transactional
    public PurchaseRequestResponse startReview(String id) {
        PurchaseRequest pr = requirePr(id);
        assertProgramAccessIfPresent(pr);
        PurchaseRequestStatus.assertTransition(pr.getStatus(), PurchaseRequestStatus.UNDER_REVIEW);
        pr.setStatus(PurchaseRequestStatus.UNDER_REVIEW);
        requestRepository.save(pr);
        return toResponse(pr, List.of("Status set to UNDER_REVIEW"));
    }

    @Transactional
    public PurchaseRequestResponse cancel(String id, String reason) {
        PurchaseRequest pr = requirePr(id);
        assertProgramAccessIfPresent(pr);
        PurchaseRequestStatus.assertTransition(pr.getStatus(), PurchaseRequestStatus.CANCELLED);
        pr.setStatus(PurchaseRequestStatus.CANCELLED);
        if (reason != null && !reason.isBlank()) {
            pr.setNotes(appendNote(pr.getNotes(), "CANCELLED: " + reason.trim()));
        }
        requestRepository.save(pr);
        return toResponse(pr, List.of("Purchase request cancelled"));
    }

    @Transactional
    public PurchaseRequestResponse reopenFromRejected(String id) {
        PurchaseRequest pr = requirePr(id);
        assertProgramAccessIfPresent(pr);
        PurchaseRequestStatus.assertTransition(pr.getStatus(), PurchaseRequestStatus.DRAFT);
        pr.setStatus(PurchaseRequestStatus.DRAFT);
        pr.setDecisionCode(null);
        pr.setDecisionReason(null);
        pr.setDecidedAt(null);
        pr.setDecidedBy(null);
        requestRepository.save(pr);
        return toResponse(pr, List.of("Rejected PR returned to DRAFT for rework"));
    }


    /**
     * Wave C: from APPROVED PR create RFQ via existing RfqService (no second RFQ system).
     * Endpoint is under PURCHASE module; internal RFQ write reuses PRODUCT_COSTING RFQ tables/services.
     * Does not open RFQ or grant portal access — buyer continues on RFQ APIs.
     */
    @Transactional
    public PurchaseRequestResponse createRfqFromApproved(String id) {
        PurchaseRequest pr = requirePr(id);
        assertProgramAccessIfPresent(pr);
        if (pr.getLinkedRfqId() != null && !pr.getLinkedRfqId().isBlank()) {
            throw new IllegalArgumentException("PR already linked to RFQ " + pr.getLinkedRfqId());
        }
        PurchaseRequestStatus.assertTransition(pr.getStatus(), PurchaseRequestStatus.RFQ_LINKED);
        if (pr.getBomNodeId() == null || pr.getBomNodeId().isBlank()) {
            throw new IllegalArgumentException("BOM node is required to create RFQ lines from PR");
        }
        List<PurchaseRequestLine> lines = lineRepository.findByPurchaseRequestIdOrderByLineNoAsc(id);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Cannot create RFQ without PR lines");
        }

        String rfqNumber = "RFQ-PR-" + (pr.getPrNumber() != null ? pr.getPrNumber().replace("PR-", "") : id.substring(0, 8));
        String title = "RFQ from " + (pr.getPrNumber() != null ? pr.getPrNumber() : id);
        String currency = pr.getReportingCurrency() != null ? pr.getReportingCurrency() : "USD";
        String notes = "Created from purchase request " + (pr.getPrNumber() != null ? pr.getPrNumber() : id)
                + (pr.getPlantCode() != null ? "; plant=" + pr.getPlantCode() : "")
                + (pr.getPurposeText() != null ? "; purpose=" + pr.getPurposeText() : "");

        RfqResponse rfq = rfqService.create(new CreateRfqRequest(
                pr.getProgramId(),
                rfqNumber,
                title,
                pr.getRequiredDate(),
                currency,
                notes
        ));

        int lineNo = 1;
        for (PurchaseRequestLine line : lines) {
            BigDecimal qty = line.getQuantity() != null ? line.getQuantity() : BigDecimal.ONE;
            String uom = line.getUom() != null ? line.getUom() : "EA";
            BigDecimal target = line.getTargetPrice() != null ? line.getTargetPrice() : line.getUnitPrice();
            String lineNotes = line.getLineNotes();
            if (line.getMaterialCode() != null) {
                lineNotes = appendNote(lineNotes, "material=" + line.getMaterialCode());
            }
            rfqService.addLine(rfq.rfqId(), new AddRfqLineRequest(
                    pr.getBomNodeId(),
                    lineNo++,
                    qty,
                    uom,
                    target,
                    lineNotes
            ));
        }

        pr.setLinkedRfqId(rfq.rfqId());
        pr.setStatus(PurchaseRequestStatus.RFQ_LINKED);
        pr.setNotes(appendNote(pr.getNotes(), "Linked RFQ " + rfq.rfqId() + " / " + rfq.rfqNumber()));
        requestRepository.save(pr);
        return toResponse(pr, List.of(
                "RFQ created via existing RFQ service: " + rfq.rfqId(),
                "RFQ number: " + rfq.rfqNumber(),
                "Open RFQ and grant supplier portal access on buyer RFQ APIs (PRODUCT_COSTING)."
        ));
    }


    private PurchaseRequest requirePr(String id) {
        String customerId = tenantAccessGuard.requireCustomerId();
        PurchaseRequest pr = requestRepository.findByCustomerIdAndPurchaseRequestId(customerId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase request", id));
        tenantAccessGuard.assertOwns(pr.getCustomerId(), "PurchaseRequest", id);
        return pr;
    }

    private BomNode requireNode(String id) {
        String customerId = tenantAccessGuard.requireCustomerId();
        return bomNodeRepository.findByCustomerIdAndBomNodeId(customerId, id)
                .orElseThrow(() -> new ResourceNotFoundException("BOM node", id));
    }

    /** Hardening D — authenticated principal name (user id / username). */
    static String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null || auth.getName().isBlank()) {
            return null;
        }
        if ("anonymousUser".equalsIgnoreCase(auth.getName())) {
            return null;
        }
        return auth.getName();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String nullToDash(String s) {
        return s == null || s.isBlank() ? "(unknown user)" : s;
    }

    private static String appendNote(String existing, String add) {
        if (existing == null || existing.isBlank()) return add;
        return existing + "; " + add;
    }

    private void assertProgramAccessIfPresent(PurchaseRequest pr) {
        if (pr.getProgramId() != null && !pr.getProgramId().isBlank()) {
            accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, pr.getProgramId());
        } else {
            String customerId = CustomerContext.getCustomerId();
            if (customerId == null || customerId.isBlank()) {
                throw new BusinessRuleViolationException("X-Customer-Id is required", "ACCESS-07");
            }
            if (pr.getCustomerId() != null && !pr.getCustomerId().equals(customerId)) {
                throw ResourceNotFoundException.of("PurchaseRequest", pr.getPurchaseRequestId());
            }
        }
    }

    private String generateStandalonePrNumber(String customerId) {
        String day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return "PR-" + day + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

}
