package com.automotive.costing.rfq.service;

import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.DuplicateResourceException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.masterdata.repository.VehicleProgramRepository;
import com.automotive.costing.rfq.dto.RfqDtos.AddRfqLineRequest;
import com.automotive.costing.rfq.dto.RfqDtos.CreateRfqRequest;
import com.automotive.costing.rfq.dto.RfqDtos.RfqDetailResponse;
import com.automotive.costing.rfq.dto.RfqDtos.RfqLineResponse;
import com.automotive.costing.rfq.dto.RfqDtos.RfqResponse;
import com.automotive.costing.rfq.entity.RfqEvent;
import com.automotive.costing.rfq.entity.RfqLine;
import com.automotive.costing.rfq.entity.RfqStatus;
import com.automotive.costing.rfq.repository.RfqEventRepository;
import com.automotive.costing.rfq.repository.RfqLineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.automotive.costing.common.event.RfqLifecycleEvent;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RfqService {

    private static final String ENTITY = "RfqEvent";

    private final RfqEventRepository rfqRepository;
    private final RfqLineRepository lineRepository;
    private final VehicleProgramRepository programRepository;
    private final BomNodeRepository bomNodeRepository;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;
    private final TenantAccessGuard tenantAccessGuard;

    @Transactional
    public RfqResponse create(CreateRfqRequest request) {
        String customerId = tenantAccessGuard.requireCustomerId();
        tenantAccessGuard.assertNotSupportReadOnly();
        if (programRepository.findByCustomerIdAndProgramId(customerId, request.programId()).isEmpty()) {
            throw ResourceNotFoundException.of("VehicleProgram", request.programId());
        }
        String number = request.rfqNumber().trim();
        if (rfqRepository.findByCustomerIdAndRfqNumber(customerId, number).isPresent()) {
            throw new DuplicateResourceException("RFQ number already exists: " + number);
        }
        RfqEvent e = new RfqEvent();
        e.setRfqId(UUID.randomUUID().toString());
        e.setCustomerId(customerId);
        e.setProgramId(request.programId());
        e.setRfqNumber(number);
        e.setTitle(request.title().trim());
        e.setStatus(RfqStatus.DRAFT);
        e.setDueDate(request.dueDate());
        e.setCurrencyCode(request.currencyCode() == null || request.currencyCode().isBlank()
                ? "USD" : request.currencyCode().trim().toUpperCase());
        e.setNotes(request.notes());
        RfqEvent saved = rfqRepository.save(e);
        auditService.recordCreate(ENTITY, saved.getRfqId(), toRfqResponse(saved));
        publishRfq("CREATED", saved.getRfqId(), null,
                "RFQ " + saved.getRfqNumber() + " created");
        return toRfqResponse(saved);
    }

    @Transactional
    public RfqResponse open(String rfqId) {
        RfqEvent e = require(rfqId);
        if (e.getStatus() != RfqStatus.DRAFT && e.getStatus() != RfqStatus.CLOSED) {
            throw new BusinessRuleViolationException("RFQ can only be opened from DRAFT or CLOSED, was " + e.getStatus(), "M09-M10");
        }
        e.setStatus(RfqStatus.OPEN);
        RfqEvent opened = rfqRepository.save(e);
        publishRfq("SENT", opened.getRfqId(), null,
                "RFQ " + opened.getRfqNumber() + " opened/sent for quoting");
        return toRfqResponse(opened);
    }

    @Transactional
    public RfqResponse close(String rfqId) {
        RfqEvent e = require(rfqId);
        if (e.getStatus() != RfqStatus.OPEN) {
            throw new BusinessRuleViolationException("Only OPEN RFQs can be closed", "M09-M10");
        }
        e.setStatus(RfqStatus.CLOSED);
        return toRfqResponse(rfqRepository.save(e));
    }

    @Transactional
    public RfqResponse cancel(String rfqId) {
        RfqEvent e = require(rfqId);
        if (e.getStatus() == RfqStatus.AWARDED) {
            throw new BusinessRuleViolationException("AWARDED RFQ cannot be cancelled", "M09-M10");
        }
        e.setStatus(RfqStatus.CANCELLED);
        return toRfqResponse(rfqRepository.save(e));
    }

    @Transactional
    public RfqLineResponse addLine(String rfqId, AddRfqLineRequest request) {
        RfqEvent e = require(rfqId);
        if (e.getStatus() == RfqStatus.CANCELLED || e.getStatus() == RfqStatus.AWARDED) {
            throw new BusinessRuleViolationException("Cannot add lines to " + e.getStatus() + " RFQ", "M09-M10");
        }
        if (bomNodeRepository.findByCustomerIdAndBomNodeId(tenantAccessGuard.requireCustomerId(), request.bomNodeId()).isEmpty()) {
            throw ResourceNotFoundException.of("BomNode", request.bomNodeId());
        }
        if (lineRepository.findByRfqIdAndBomNodeId(rfqId, request.bomNodeId()).isPresent()) {
            throw new DuplicateResourceException("BOM node already on this RFQ: " + request.bomNodeId());
        }
        int lineNo = request.lineNumber() != null
                ? request.lineNumber()
                : lineRepository.findByRfqIdOrderByLineNumberAsc(rfqId).stream()
                        .mapToInt(RfqLine::getLineNumber)
                        .max()
                        .orElse(0) + 1;

        RfqLine line = new RfqLine();
        line.setCustomerId(tenantAccessGuard.requireCustomerId());
        line.setRfqLineId(UUID.randomUUID().toString());
        line.setRfqId(rfqId);
        line.setBomNodeId(request.bomNodeId());
        line.setLineNumber(lineNo);
        line.setQuantity(request.quantity() != null ? request.quantity() : BigDecimal.ONE);
        line.setUom(request.uom());
        line.setTargetUnitPrice(request.targetUnitPrice());
        line.setNotes(request.notes());
        return toLineResponse(lineRepository.save(line));
    }

    @Transactional(readOnly = true)
    public RfqDetailResponse getDetail(String rfqId) {
        RfqEvent e = require(rfqId);
        List<RfqLineResponse> lines = lineRepository.findByRfqIdOrderByLineNumberAsc(rfqId).stream()
                .map(this::toLineResponse)
                .toList();
        return new RfqDetailResponse(toRfqResponse(e), lines);
    }

    @Transactional(readOnly = true)
    public List<RfqResponse> listByProgram(String programId) {
        String customerId = tenantAccessGuard.requireCustomerId();
        return rfqRepository.findByCustomerIdAndProgramIdOrderByCreatedAtDesc(customerId, programId).stream()
                .map(this::toRfqResponse)
                .toList();
    }

    public RfqEvent require(String rfqId) {
        String customerId = tenantAccessGuard.requireCustomerId();
        RfqEvent e = rfqRepository.findByCustomerIdAndRfqId(customerId, rfqId)
                .orElseThrow(() -> ResourceNotFoundException.of(ENTITY, rfqId));
        tenantAccessGuard.assertOwns(e.getCustomerId(), ENTITY, rfqId);
        return e;
    }

    private RfqResponse toRfqResponse(RfqEvent e) {
        return new RfqResponse(
                e.getRfqId(), e.getProgramId(), e.getRfqNumber(), e.getTitle(), e.getStatus(),
                e.getDueDate(), e.getCurrencyCode(), e.getNotes(), e.getCreatedAt());
    }

    private RfqLineResponse toLineResponse(RfqLine l) {
        return new RfqLineResponse(
                l.getRfqLineId(), l.getRfqId(), l.getBomNodeId(), l.getLineNumber(),
                l.getQuantity(), l.getUom(), l.getTargetUnitPrice(), l.getNotes());
    }

    private void publishRfq(String kind, String rfqId, String quoteId, String summary) {
        String recipient = currentUserId();
        if (recipient == null) {
            return;
        }
        eventPublisher.publishEvent(new RfqLifecycleEvent(kind, rfqId, quoteId, recipient, summary));
    }

    private static String currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || auth.getName().isBlank()) {
            return null;
        }
        return auth.getName();
    }
}
