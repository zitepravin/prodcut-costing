package com.automotive.costing.purchase.service;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.notification.entity.NotificationSeverity;
import com.automotive.costing.notification.entity.NotificationType;
import com.automotive.costing.notification.service.NotificationService;
import com.automotive.costing.platform.access.CustomerContext;
import com.automotive.costing.common.outbox.DomainOutboxService;
import com.automotive.costing.platform.user.repository.UserRepository;
import com.automotive.costing.purchase.dto.PurchaseOrderDtos.CreateFromPurchaseRequestRequest;
import com.automotive.costing.purchase.dto.PurchaseOrderDtos.ClosePoRequest;
import com.automotive.costing.purchase.dto.PurchaseOrderDtos.CreatePurchaseOrderRequest;
import com.automotive.costing.purchase.dto.PurchaseOrderDtos.DecidePoRequest;
import com.automotive.costing.purchase.dto.PurchaseOrderDtos.IssuePoRequest;
import com.automotive.costing.purchase.dto.PurchaseOrderDtos.PoLineInput;
import com.automotive.costing.purchase.dto.PurchaseOrderDtos.PoLineResponse;
import com.automotive.costing.purchase.dto.PurchaseOrderDtos.PurchaseOrderResponse;
import com.automotive.costing.purchase.entity.PurchaseOrder;
import com.automotive.costing.purchase.entity.PurchaseOrderLine;
import com.automotive.costing.purchase.entity.PurchaseRequest;
import com.automotive.costing.purchase.repository.PurchaseOrderLineRepository;
import com.automotive.costing.purchase.repository.PurchaseOrderRepository;
import com.automotive.costing.purchase.repository.PurchaseRequestLineRepository;
import com.automotive.costing.purchase.repository.PurchaseRequestRepository;
import com.automotive.costing.purchase.entity.PurchaseRequestLine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Wave D — Purchase Order lifecycle under module PURCHASE.
 * Human-only approve/issue. Does not post to SAP MM. Does not invent prices/FX.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderService {

    private static final String ENTITY = "PurchaseOrder";

    private final PurchaseOrderRepository orderRepository;
    private final PurchaseOrderLineRepository lineRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final PurchaseRequestLineRepository purchaseRequestLineRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final DomainOutboxService domainOutboxService;


    /**
     * Next purchase development: seed DRAFT PO from PR lines.
     * Allowed PR status: APPROVED | RFQ_LINKED | RELEASED.
     * Does not auto-issue; human must submit → decide → issue.
     */
    @Transactional
    public PurchaseOrderResponse createFromPurchaseRequest(String purchaseRequestId, CreateFromPurchaseRequestRequest request) {
        String customerId = requireCustomerId();
        if (request == null || request.supplierId() == null || request.supplierId().isBlank()) {
            throw new BusinessRuleViolationException("supplierId is required", "PO_SUPPLIER_REQUIRED");
        }
        PurchaseRequest pr = purchaseRequestRepository.findById(purchaseRequestId)
                .orElseThrow(() -> ResourceNotFoundException.of("PurchaseRequest", purchaseRequestId));
        if (pr.getCustomerId() != null && !pr.getCustomerId().isBlank()
                && !pr.getCustomerId().equals(customerId)) {
            throw ResourceNotFoundException.of("PurchaseRequest", purchaseRequestId);
        }
        String st = PurchaseRequestStatus.norm(pr.getStatus());
        if (!List.of(
                PurchaseRequestStatus.APPROVED,
                PurchaseRequestStatus.RFQ_LINKED,
                PurchaseRequestStatus.RELEASED
        ).contains(st)) {
            throw new BusinessRuleViolationException(
                    "Purchase request must be APPROVED, RFQ_LINKED, or RELEASED to create a PO (was " + st + ")",
                    "PO_PR_STATUS");
        }

        List<PurchaseRequestLine> prLines =
                purchaseRequestLineRepository.findByPurchaseRequestIdOrderByLineNoAsc(purchaseRequestId);
        if (prLines == null || prLines.isEmpty()) {
            throw new BusinessRuleViolationException(
                    "Purchase request has no lines — cannot create PO", "PO_PR_NO_LINES");
        }

        List<PoLineInput> lines = new ArrayList<>();
        for (PurchaseRequestLine pl : prLines) {
            lines.add(new PoLineInput(
                    pl.getMaterialCode(),
                    pl.getMaterialDescription(),
                    pl.getQuantity() != null ? pl.getQuantity() : BigDecimal.ONE,
                    pl.getUom() != null ? pl.getUom() : "EA",
                    pl.getUnitPrice(),
                    pl.getCurrency() != null ? pl.getCurrency() : pr.getReportingCurrency(),
                    null,
                    null,
                    null,
                    pr.getPlantCode(),
                    null,
                    null
            ));
        }

        String currency = blankToNull(request.currencyCode());
        if (currency == null) {
            currency = pr.getReportingCurrency() != null ? pr.getReportingCurrency() : "USD";
        }

        CreatePurchaseOrderRequest createReq = new CreatePurchaseOrderRequest(
                purchaseRequestId,
                pr.getLinkedRfqId(),
                request.supplierId().trim(),
                blankToNull(request.plantCode()) != null ? blankToNull(request.plantCode()) : pr.getPlantCode(),
                null,
                currency,
                blankToNull(request.paymentTerms()),
                blankToNull(request.incoterms()),
                null,
                null,
                pr.getRequiredDate(),
                blankToNull(request.notes()),
                lines
        );
        return create(createReq);
    }

    @Transactional
    public PurchaseOrderResponse create(CreatePurchaseOrderRequest request) {
        String customerId = requireCustomerId();
        String actor = currentUserId();

        if (request.supplierId() == null || request.supplierId().isBlank()) {
            throw new BusinessRuleViolationException("supplierId is required", "PO_SUPPLIER_REQUIRED");
        }
        if (request.lines() == null || request.lines().isEmpty()) {
            throw new BusinessRuleViolationException("At least one PO line is required", "PO_LINES_REQUIRED");
        }

        String prId = blankToNull(request.purchaseRequestId());
        String copiedExternalPoRef = null;
        if (prId != null) {
            PurchaseRequest pr = purchaseRequestRepository.findById(prId)
                    .orElseThrow(() -> ResourceNotFoundException.of("PurchaseRequest", prId));
            String st = PurchaseRequestStatus.norm(pr.getStatus());
            if (!List.of(
                    PurchaseRequestStatus.APPROVED,
                    PurchaseRequestStatus.RFQ_LINKED,
                    PurchaseRequestStatus.RELEASED
            ).contains(st)) {
                throw new BusinessRuleViolationException(
                        "Purchase request must be APPROVED, RFQ_LINKED, or RELEASED to seed a PO",
                        "PO_PR_STATUS");
            }
            // Reference copy only — SAP link API externalPoNumber maps to PR.externalPoReference;
            // "SAP_MM" on PR is a label, not a live connection. No outbound SAP call here.
            if (pr.getExternalPoReference() != null && !pr.getExternalPoReference().isBlank()) {
                copiedExternalPoRef = pr.getExternalPoReference().trim();
            }
        }

        String currency = blankToNull(request.currencyCode());
        if (currency == null) {
            currency = "USD";
        }
        currency = currency.toUpperCase(Locale.ROOT);

        PurchaseOrder po = new PurchaseOrder();
        po.setPurchaseOrderId(UUID.randomUUID().toString());
        po.setPoNumber(generatePoNumber(customerId));
        po.setCustomerId(customerId);
        po.setPurchaseRequestId(prId);
        po.setLinkedRfqId(blankToNull(request.linkedRfqId()));
        po.setSupplierId(request.supplierId().trim());
        po.setPlantCode(blankToNull(request.plantCode()));
        po.setBuyerUserId(blankToNull(request.buyerUserId()) != null
                ? blankToNull(request.buyerUserId()) : actor);
        po.setCurrencyCode(currency);
        po.setPaymentTerms(blankToNull(request.paymentTerms()));
        po.setIncoterms(blankToNull(request.incoterms()));
        po.setDeliveryTerms(blankToNull(request.deliveryTerms()));
        po.setOrderDate(request.orderDate() != null ? request.orderDate() : LocalDate.now(ZoneOffset.UTC));
        po.setRequiredDeliveryDate(request.requiredDeliveryDate());
        po.setStatus(PurchaseOrderStatus.DRAFT);
        po.setNotes(blankToNull(request.notes()));
        if (copiedExternalPoRef != null) {
            po.setExternalPoReference(copiedExternalPoRef);
        }
        po.setCreatedBy(actor);
        po.setUpdatedBy(actor);
        orderRepository.save(po);

        saveLines(po, request.lines(), actor, true);
        List<String> messages = new ArrayList<>();
        messages.add("Purchase order created as DRAFT. Submit for approval when commercial data is complete.");
        if (copiedExternalPoRef != null) {
            messages.add("Copied externalPoReference from purchase request (reference only — no SAP MM post).");
        }
        return toResponse(po, messages);
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderResponse> list(String statusFilter) {
        String customerId = requireCustomerId();
        List<PurchaseOrder> list;
        if (statusFilter != null && !statusFilter.isBlank()) {
            list = orderRepository.findByCustomerIdAndStatusOrderByCreatedAtDesc(
                    customerId, PurchaseOrderStatus.norm(statusFilter));
        } else {
            list = orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
        }
        return list.stream().map(po -> toResponse(po, List.of())).toList();
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponse get(String id) {
        return toResponse(requireOrder(id), List.of());
    }

    @Transactional
    public PurchaseOrderResponse submit(String id) {
        PurchaseOrder po = requireOrder(id);
        PurchaseOrderStatus.assertTransition(po.getStatus(), PurchaseOrderStatus.SUBMITTED);
        assertLinesPresent(po);
        assertNoMissingPriceBlocksIssue(po, false);

        po.setStatus(PurchaseOrderStatus.SUBMITTED);
        po.setUpdatedBy(currentUserId());
        orderRepository.save(po);

        // Auto-move to APPROVAL queue (buyer submitted → pending approver)
        PurchaseOrderStatus.assertTransition(PurchaseOrderStatus.SUBMITTED, PurchaseOrderStatus.APPROVAL);
        po.setStatus(PurchaseOrderStatus.APPROVAL);
        orderRepository.save(po);

        notify(po, NotificationType.PURCHASE_ORDER_SUBMITTED,
                "Purchase order submitted",
                "PO " + displayPo(po) + " submitted and pending approval.",
                NotificationSeverity.INFO);
        notify(po, NotificationType.PURCHASE_ORDER_APPROVAL_REQUIRED,
                "Purchase order approval required",
                "PO " + displayPo(po) + " requires human approval.",
                NotificationSeverity.WARNING);

        return toResponse(po, List.of("Submitted — status APPROVAL (human decision required)."));
    }

    @Transactional
    public PurchaseOrderResponse decide(String id, DecidePoRequest request) {
        PurchaseOrder po = requireOrder(id);
        PurchaseOrderStatus.assertTransition(po.getStatus(), 
                "APPROVE".equalsIgnoreCase(request.decisionCode()) 
                        ? PurchaseOrderStatus.APPROVED 
                        : PurchaseOrderStatus.REJECTED);

        String code = request.decisionCode() == null ? "" : request.decisionCode().trim().toUpperCase(Locale.ROOT);
        if (!List.of("APPROVE", "REJECT").contains(code)) {
            throw new BusinessRuleViolationException("decisionCode must be APPROVE or REJECT", "PO_DECISION_CODE");
        }
        if (request.decisionReason() == null || request.decisionReason().isBlank()) {
            throw new BusinessRuleViolationException("decisionReason is required", "PO_DECISION_REASON");
        }

        String actor = currentUserId();
        po.setDecisionReason(request.decisionReason().trim());
        po.setDecidedBy(actor);
        po.setDecidedAt(Instant.now());
        po.setUpdatedBy(actor);

        if ("APPROVE".equals(code)) {
            po.setStatus(PurchaseOrderStatus.APPROVED);
            orderRepository.save(po);
            domainOutboxService.enqueue(
                    ENTITY,
                    po.getPurchaseOrderId(),
                    "PURCHASE_ORDER_APPROVED",
                    Map.of(
                            "purchaseOrderId", po.getPurchaseOrderId(),
                            "poNumber", po.getPoNumber() != null ? po.getPoNumber() : "",
                            "customerId", po.getCustomerId() != null ? po.getCustomerId() : "",
                            "status", po.getStatus(),
                            "decisionCode", "APPROVE"
                    ));
            notify(po, NotificationType.PURCHASE_ORDER_APPROVED,
                    "Purchase order approved",
                    "PO " + displayPo(po) + " approved. Ready to issue.",
                    NotificationSeverity.INFO);
            return toResponse(po, List.of("Approved — human decision recorded."));
        }

        po.setStatus(PurchaseOrderStatus.REJECTED);
        orderRepository.save(po);
        notify(po, NotificationType.PURCHASE_ORDER_REJECTED,
                "Purchase order rejected",
                "PO " + displayPo(po) + " rejected: " + po.getDecisionReason(),
                NotificationSeverity.WARNING);
        return toResponse(po, List.of("Rejected — move to REWORK to edit, or CANCEL."));
    }

    @Transactional
    public PurchaseOrderResponse rework(String id) {
        PurchaseOrder po = requireOrder(id);
        PurchaseOrderStatus.assertTransition(po.getStatus(), PurchaseOrderStatus.REWORK);
        po.setStatus(PurchaseOrderStatus.REWORK);
        po.setUpdatedBy(currentUserId());
        orderRepository.save(po);
        return toResponse(po, List.of("Status REWORK — edit lines then re-submit."));
    }

    @Transactional
    public PurchaseOrderResponse issue(String id, IssuePoRequest request) {
        PurchaseOrder po = requireOrder(id);
        PurchaseOrderStatus.assertTransition(po.getStatus(), PurchaseOrderStatus.ISSUED);
        assertLinesPresent(po);
        assertNoMissingPriceBlocksIssue(po, true);

        String actor = currentUserId();
        po.setStatus(PurchaseOrderStatus.ISSUED);
        po.setIssuedAt(Instant.now());
        po.setIssuedBy(actor);
        if (request != null && blankToNull(request.externalPoReference()) != null) {
            po.setExternalPoReference(blankToNull(request.externalPoReference()));
        }
        po.setUpdatedBy(actor);
        orderRepository.save(po);

        domainOutboxService.enqueue(
                ENTITY,
                po.getPurchaseOrderId(),
                "PURCHASE_ORDER_ISSUED",
                Map.of(
                        "purchaseOrderId", po.getPurchaseOrderId(),
                        "poNumber", po.getPoNumber() != null ? po.getPoNumber() : "",
                        "customerId", po.getCustomerId() != null ? po.getCustomerId() : "",
                        "status", po.getStatus()
                ));

        notify(po, NotificationType.PURCHASE_ORDER_ISSUED,
                "Purchase order issued",
                "PO " + displayPo(po) + " issued (export/reference only — does not post to SAP MM).",
                NotificationSeverity.INFO);
        return toResponse(po, List.of(
                "Issued. externalPoReference may be set for CPI/ERP handoff.",
                "This platform does not post to SAP MM / ME21N."
        ));
    }

    @Transactional
    public PurchaseOrderResponse cancel(String id, String reason) {
        PurchaseOrder po = requireOrder(id);
        PurchaseOrderStatus.assertTransition(po.getStatus(), PurchaseOrderStatus.CANCELLED);
        po.setStatus(PurchaseOrderStatus.CANCELLED);
        if (reason != null && !reason.isBlank()) {
            po.setNotes((po.getNotes() == null ? "" : po.getNotes() + " | ") + "CANCEL: " + reason.trim());
        }
        po.setUpdatedBy(currentUserId());
        orderRepository.save(po);
        return toResponse(po, List.of("Cancelled."));
    }

    /**
     * Light operational close (not ERP GR/IR).
     * ISSUED → PARTIALLY_RECEIVED (partial=true) or CLOSED.
     * PARTIALLY_RECEIVED → CLOSED.
     */
    @Transactional
    public PurchaseOrderResponse close(String id, ClosePoRequest request) {
        PurchaseOrder po = requireOrder(id);
        boolean partial = request != null && request.partial();
        String target = partial ? PurchaseOrderStatus.PARTIALLY_RECEIVED : PurchaseOrderStatus.CLOSED;
        try {
            PurchaseOrderStatus.assertTransition(po.getStatus(), target);
        } catch (IllegalArgumentException ex) {
            throw new BusinessRuleViolationException(ex.getMessage(), "PO_STATUS");
        }
        String actor = currentUserId();
        po.setStatus(target);
        po.setUpdatedBy(actor);
        if (request != null && request.reason() != null && !request.reason().isBlank()) {
            String tag = partial ? "PARTIAL_RECEIPT: " : "CLOSED: ";
            po.setNotes((po.getNotes() == null ? "" : po.getNotes() + " | ") + tag + request.reason().trim());
        }
        orderRepository.save(po);
        if (partial) {
            notify(po, NotificationType.PURCHASE_ORDER_PARTIALLY_RECEIVED,
                    "Purchase order partially received",
                    "PO " + displayPo(po) + " marked partially received (operational).",
                    NotificationSeverity.INFO);
            return toResponse(po, List.of(
                    "Status PARTIALLY_RECEIVED — light operational marker only.",
                    "This is not a full ERP goods receipt / IR posting."
            ));
        }
        notify(po, NotificationType.PURCHASE_ORDER_CLOSED,
                "Purchase order closed",
                "PO " + displayPo(po) + " closed.",
                NotificationSeverity.INFO);
        return toResponse(po, List.of(
                "Status CLOSED — operational close complete.",
                "This is not a full ERP GR/IR clone."
        ));
    }


    // --- helpers ---

    private void saveLines(PurchaseOrder po, List<PoLineInput> inputs, String actor, boolean replace) {
        if (replace) {
            lineRepository.deleteByPurchaseOrderId(po.getPurchaseOrderId());
        }
        AtomicInteger no = new AtomicInteger(1);
        for (PoLineInput in : inputs) {
            if (in.quantity() == null || in.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRuleViolationException(
                        "Line quantity must be > 0", "PO_LINE_QTY");
            }
            PurchaseOrderLine line = new PurchaseOrderLine();
            line.setLineId(UUID.randomUUID().toString());
            line.setPurchaseOrderId(po.getPurchaseOrderId());
            line.setLineNo(no.getAndIncrement());
            line.setMaterialCode(blankToNull(in.materialCode()));
            line.setMaterialDescription(blankToNull(in.materialDescription()));
            line.setQuantity(in.quantity());
            line.setUom(blankToNull(in.uom()) != null ? in.uom().trim() : "EA");
            line.setUnitPrice(in.unitPrice());
            line.setCurrencyCode(blankToNull(in.currencyCode()) != null
                    ? in.currencyCode().trim().toUpperCase(Locale.ROOT)
                    : po.getCurrencyCode());
            line.setTaxAmount(in.taxAmount());
            line.setDiscountAmount(in.discountAmount());
            line.setDeliveryDate(in.deliveryDate());
            line.setPlantCode(blankToNull(in.plantCode()) != null ? blankToNull(in.plantCode()) : po.getPlantCode());
            line.setStorageLocation(blankToNull(in.storageLocation()));
            line.setLineNotes(blankToNull(in.lineNotes()));
            if (in.unitPrice() != null && in.unitPrice().compareTo(BigDecimal.ZERO) >= 0) {
                line.setDataStatus("AVAILABLE");
            } else {
                line.setDataStatus("NOT_AVAILABLE");
            }
            line.setCreatedBy(actor);
            line.setUpdatedBy(actor);
            lineRepository.save(line);
        }
    }

    private void assertLinesPresent(PurchaseOrder po) {
        List<PurchaseOrderLine> lines = lineRepository.findByPurchaseOrderIdOrderByLineNoAsc(po.getPurchaseOrderId());
        if (lines.isEmpty()) {
            throw new BusinessRuleViolationException("PO has no lines", "PO_NO_LINES");
        }
    }

    /**
     * Issue requires all lines AVAILABLE (no invented price).
     * Submit only warns via message path when strict=false — still blocks if zero lines.
     */
    private void assertNoMissingPriceBlocksIssue(PurchaseOrder po, boolean strict) {
        List<PurchaseOrderLine> lines = lineRepository.findByPurchaseOrderIdOrderByLineNoAsc(po.getPurchaseOrderId());
        boolean missing = lines.stream().anyMatch(l ->
                l.getUnitPrice() == null || "NOT_AVAILABLE".equalsIgnoreCase(l.getDataStatus()));
        if (missing && strict) {
            throw new BusinessRuleViolationException(
                    "Cannot issue PO while any line unit price is NOT_AVAILABLE",
                    "PO_PRICE_NOT_AVAILABLE");
        }
    }

    private PurchaseOrder requireOrder(String id) {
        String customerId = requireCustomerId();
        return orderRepository.findByPurchaseOrderIdAndCustomerId(id, customerId)
                .orElseThrow(() -> ResourceNotFoundException.of(ENTITY, id));
    }

    private String requireCustomerId() {
        String cid = CustomerContext.getCustomerId();
        if (cid == null || cid.isBlank()) {
            throw new BusinessRuleViolationException(
                    "X-Customer-Id is required for purchase order operations", "ACCESS-07");
        }
        return cid;
    }

    private String generatePoNumber(String customerId) {
        String stamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());
        String candidate = "PO-" + stamp;
        int i = 0;
        while (orderRepository.existsByCustomerIdAndPoNumber(customerId, candidate) && i < 20) {
            i++;
            candidate = "PO-" + stamp + "-" + i;
        }
        return candidate;
    }

    private void notify(PurchaseOrder po, NotificationType type, String title, String body,
                        NotificationSeverity severity) {
        String recipient = po.getCreatedBy();
        if (recipient == null || recipient.isBlank()) {
            recipient = po.getBuyerUserId();
        }
        if (recipient == null || recipient.isBlank()) {
            return;
        }
        try {
            if (userRepository.findById(recipient).isEmpty()) {
                return;
            }
            notificationService.publish(recipient, type, title, body, ENTITY, po.getPurchaseOrderId(), severity);
        } catch (Exception ex) {
            log.warn("Could not publish {} for PO {}: {}", type, po.getPurchaseOrderId(), ex.getMessage());
        }
    }

    private static String displayPo(PurchaseOrder po) {
        if (po.getPoNumber() != null && !po.getPoNumber().isBlank()) {
            return po.getPoNumber();
        }
        return po.getPurchaseOrderId();
    }

    private PurchaseOrderResponse toResponse(PurchaseOrder po, List<String> messages) {
        List<PoLineResponse> lines = lineRepository
                .findByPurchaseOrderIdOrderByLineNoAsc(po.getPurchaseOrderId()).stream()
                .map(this::toLine)
                .toList();
        return new PurchaseOrderResponse(
                po.getPurchaseOrderId(),
                po.getPoNumber(),
                po.getCustomerId(),
                po.getPurchaseRequestId(),
                po.getLinkedRfqId(),
                po.getSupplierId(),
                po.getPlantCode(),
                po.getBuyerUserId(),
                po.getCurrencyCode(),
                po.getPaymentTerms(),
                po.getIncoterms(),
                po.getDeliveryTerms(),
                po.getOrderDate(),
                po.getRequiredDeliveryDate(),
                po.getStatus(),
                po.getDecisionReason(),
                po.getDecidedBy(),
                po.getDecidedAt(),
                po.getIssuedAt(),
                po.getIssuedBy(),
                po.getExternalPoReference(),
                po.getNotes(),
                lines,
                messages
        );
    }

    private PoLineResponse toLine(PurchaseOrderLine l) {
        return new PoLineResponse(
                l.getLineId(),
                l.getLineNo(),
                l.getMaterialCode(),
                l.getMaterialDescription(),
                l.getQuantity(),
                l.getUom(),
                l.getUnitPrice(),
                l.getCurrencyCode(),
                l.getTaxAmount(),
                l.getDiscountAmount(),
                l.getDeliveryDate(),
                l.getPlantCode(),
                l.getStorageLocation(),
                l.getDataStatus(),
                l.getLineNotes()
        );
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    private static String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            return null;
        }
        return auth.getName();
    }
}
