package com.automotive.costing.support.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.notification.entity.NotificationSeverity;
import com.automotive.costing.notification.entity.NotificationType;
import com.automotive.costing.notification.service.NotificationService;
import com.automotive.costing.platform.access.CustomerContext;
import com.automotive.costing.platform.user.entity.User;
import com.automotive.costing.support.dto.SupportTicketDtos.*;
import com.automotive.costing.support.entity.*;
import com.automotive.costing.support.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final SupportTicketMessageRepository messageRepository;
    private final SupportTicketInternalNoteRepository noteRepository;
    private final SupportTicketSeqRepository seqRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;

    private String requireCustomerId() {
        String cid = CustomerContext.getCustomerId();
        if (cid == null || cid.isBlank()) {
            throw new BusinessRuleViolationException("Customer context required", "SUPPORT-10");
        }
        return cid;
    }

    public void assertCustomerCanAccess(String ticketId) {
        loadCustomerTicket(ticketId);
    }

    @Transactional
    public TicketDetail create(User user, CreateRequest req) {
        String customerId = requireCustomerId();
        SupportTicket t = new SupportTicket();
        t.setTicketId(UUID.randomUUID().toString());
        t.setTicketNumber(seqRepository.nextTicketNumber());
        t.setCustomerId(customerId);
        t.setCreatedByUserId(user.getUserId());
        t.setSubject(req.subject().trim());
        t.setDescription(req.description().trim());
        t.setCategory(req.category());
        t.setPriority(req.priority());
        t.setStatus(SupportTicketStatus.OPEN);
        ticketRepository.save(t);

        SupportTicketMessage initial = new SupportTicketMessage();
        initial.setMessageId(UUID.randomUUID().toString());
        initial.setTicketId(t.getTicketId());
        initial.setAuthorUserId(user.getUserId());
        initial.setAuthorType(SupportAuthorType.CUSTOMER);
        initial.setMessageBody(req.description().trim());
        initial.setCreatedAt(Instant.now());
        messageRepository.save(initial);

        auditService.recordCreate("SupportTicket", t.getTicketId(),
                Map.of("ticketNumber", t.getTicketNumber(), "customerId", customerId, "subject", t.getSubject()));

        notify(user.getUserId(), NotificationType.SUPPORT_TICKET_CREATED,
                "Support ticket " + t.getTicketNumber(),
                "Your ticket was created: " + t.getSubject(),
                t.getTicketId(), NotificationSeverity.INFO);

        return toDetail(t, false);
    }

    @Transactional(readOnly = true)
    public Page<TicketSummary> listMine(User user, SupportTicketStatus status,
                                        SupportTicketPriority priority,
                                        SupportTicketCategory category,
                                        String q, Pageable pageable) {
        String customerId = requireCustomerId();
        // Load page then filter in-memory for optional fields (indexed customer_id path remains primary)
        Page<SupportTicket> page = status != null
                ? ticketRepository.findByCustomerIdAndStatusOrderByUpdatedAtDesc(customerId, status, pageable)
                : ticketRepository.findByCustomerIdOrderByUpdatedAtDesc(customerId, pageable);
        List<SupportTicket> filtered = page.getContent().stream()
                .filter(t -> priority == null || t.getPriority() == priority)
                .filter(t -> category == null || t.getCategory() == category)
                .filter(t -> q == null || q.isBlank()
                        || t.getSubject().toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT))
                        || t.getTicketNumber().toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
        if (priority == null && category == null && (q == null || q.isBlank())) {
            return page.map(this::toSummary);
        }
        return new PageImpl<>(filtered.stream().map(this::toSummary).toList(), pageable, filtered.size());
    }

    @Transactional(readOnly = true)
    public TicketDetail getMine(User user, String ticketId) {
        return toDetail(loadCustomerTicket(ticketId), false);
    }

    @Transactional
    public MessageResponse replyCustomer(User user, String ticketId, MessageRequest req) {
        SupportTicket t = loadCustomerTicket(ticketId);
        if (t.getStatus() == SupportTicketStatus.CLOSED) {
            throw new BusinessRuleViolationException("Cannot reply to a closed ticket", "SUPPORT-20");
        }
        SupportTicketMessage m = saveMessage(t.getTicketId(), user.getUserId(), SupportAuthorType.CUSTOMER, req.message());
        if (t.getStatus() == SupportTicketStatus.WAITING_FOR_CUSTOMER) {
            t.setStatus(SupportTicketStatus.IN_PROGRESS);
        }
        ticketRepository.save(t);
        auditService.recordUpdate("SupportTicketMessage", m.getMessageId(),
                null, Map.of("ticketId", ticketId, "authorType", "CUSTOMER"));
        if (t.getAssignedToUserId() != null) {
            notify(t.getAssignedToUserId(), NotificationType.SUPPORT_TICKET_REPLY,
                    "Reply on " + t.getTicketNumber(), req.message().trim(),
                    t.getTicketId(), NotificationSeverity.INFO);
        }
        return new MessageResponse(m.getMessageId(), m.getAuthorUserId(), m.getAuthorType(),
                m.getMessageBody(), m.getCreatedAt());
    }

    @Transactional
    public TicketDetail closeCustomer(User user, String ticketId) {
        SupportTicket t = loadCustomerTicket(ticketId);
        if (t.getStatus() == SupportTicketStatus.CLOSED) {
            return toDetail(t, false);
        }
        SupportTicketStatus old = t.getStatus();
        t.setStatus(SupportTicketStatus.CLOSED);
        t.setClosedAt(Instant.now());
        ticketRepository.save(t);
        auditService.recordUpdate("SupportTicket", t.getTicketId(),
                Map.of("status", old.name()), Map.of("status", "CLOSED"));
        notify(user.getUserId(), NotificationType.SUPPORT_TICKET_CLOSED,
                "Ticket closed " + t.getTicketNumber(), t.getSubject(),
                t.getTicketId(), NotificationSeverity.INFO);
        return toDetail(t, false);
    }

    @Transactional(readOnly = true)
    public Page<PlatformTicketSummary> listAll(String q, String customerId, Pageable pageable) {
        Page<SupportTicket> page = ticketRepository.findAllByOrderByUpdatedAtDesc(pageable);
        List<SupportTicket> filtered = page.getContent().stream()
                .filter(t -> customerId == null || customerId.isBlank() || customerId.equals(t.getCustomerId()))
                .filter(t -> q == null || q.isBlank()
                        || t.getSubject().toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT))
                        || t.getTicketNumber().toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT)))
                .toList();
        if ((q == null || q.isBlank()) && (customerId == null || customerId.isBlank())) {
            return page.map(this::toPlatformSummary);
        }
        return new PageImpl<>(filtered.stream().map(this::toPlatformSummary).toList(), pageable, filtered.size());
    }

    @Transactional(readOnly = true)
    public TicketDetail getPlatform(String ticketId) {
        SupportTicket t = ticketRepository.findById(ticketId)
                .orElseThrow(() -> ResourceNotFoundException.of("SupportTicket", ticketId));
        return toDetail(t, true);
    }

    @Transactional
    public MessageResponse replySupport(User user, String ticketId, MessageRequest req) {
        SupportTicket t = ticketRepository.findById(ticketId)
                .orElseThrow(() -> ResourceNotFoundException.of("SupportTicket", ticketId));
        SupportTicketMessage m = saveMessage(t.getTicketId(), user.getUserId(), SupportAuthorType.SUPPORT, req.message());
        if (t.getStatus() == SupportTicketStatus.OPEN) {
            t.setStatus(SupportTicketStatus.IN_PROGRESS);
        }
        ticketRepository.save(t);
        auditService.recordUpdate("SupportTicketMessage", m.getMessageId(),
                null, Map.of("ticketId", ticketId, "authorType", "SUPPORT"));
        notify(t.getCreatedByUserId(), NotificationType.SUPPORT_TICKET_REPLY,
                "Support replied on " + t.getTicketNumber(), req.message().trim(),
                t.getTicketId(), NotificationSeverity.INFO);
        return new MessageResponse(m.getMessageId(), m.getAuthorUserId(), m.getAuthorType(),
                m.getMessageBody(), m.getCreatedAt());
    }

    @Transactional
    public TicketDetail assign(User user, String ticketId, AssignRequest req) {
        SupportTicket t = ticketRepository.findById(ticketId)
                .orElseThrow(() -> ResourceNotFoundException.of("SupportTicket", ticketId));
        String old = t.getAssignedToUserId();
        t.setAssignedToUserId(req.assigneeUserId());
        if (t.getStatus() == SupportTicketStatus.OPEN) {
            t.setStatus(SupportTicketStatus.IN_PROGRESS);
        }
        ticketRepository.save(t);
        auditService.recordUpdate("SupportTicket", t.getTicketId(),
                Map.of("assignedTo", String.valueOf(old)),
                Map.of("assignedTo", req.assigneeUserId()));
        notify(req.assigneeUserId(), NotificationType.SUPPORT_TICKET_ASSIGNED,
                "Assigned " + t.getTicketNumber(), t.getSubject(),
                t.getTicketId(), NotificationSeverity.INFO);
        return toDetail(t, true);
    }

    @Transactional
    public TicketDetail changeStatus(User user, String ticketId, StatusRequest req) {
        SupportTicket t = ticketRepository.findById(ticketId)
                .orElseThrow(() -> ResourceNotFoundException.of("SupportTicket", ticketId));
        SupportTicketStatus old = t.getStatus();
        validatePlatformTransition(old, req.status());
        t.setStatus(req.status());
        if (req.status() == SupportTicketStatus.RESOLVED) {
            t.setResolvedAt(Instant.now());
        }
        if (req.status() == SupportTicketStatus.CLOSED) {
            t.setClosedAt(Instant.now());
        }
        ticketRepository.save(t);
        auditService.recordUpdate("SupportTicket", t.getTicketId(),
                Map.of("status", old.name()), Map.of("status", req.status().name()));
        NotificationType nt = switch (req.status()) {
            case RESOLVED -> NotificationType.SUPPORT_TICKET_RESOLVED;
            case CLOSED -> NotificationType.SUPPORT_TICKET_CLOSED;
            default -> NotificationType.SUPPORT_TICKET_STATUS_CHANGED;
        };
        notify(t.getCreatedByUserId(), nt,
                "Ticket " + t.getTicketNumber() + " → " + req.status().name(),
                t.getSubject(), t.getTicketId(), NotificationSeverity.INFO);
        return toDetail(t, true);
    }

    @Transactional
    public InternalNoteResponse addInternalNote(User user, String ticketId, InternalNoteRequest req) {
        ticketRepository.findById(ticketId)
                .orElseThrow(() -> ResourceNotFoundException.of("SupportTicket", ticketId));
        SupportTicketInternalNote n = new SupportTicketInternalNote();
        n.setNoteId(UUID.randomUUID().toString());
        n.setTicketId(ticketId);
        n.setAuthorUserId(user.getUserId());
        n.setNoteBody(req.note().trim());
        n.setCreatedAt(Instant.now());
        noteRepository.save(n);
        auditService.recordCreate("SupportTicketInternalNote", n.getNoteId(),
                Map.of("ticketId", ticketId));
        return new InternalNoteResponse(n.getNoteId(), n.getAuthorUserId(), n.getNoteBody(), n.getCreatedAt());
    }

    private void validatePlatformTransition(SupportTicketStatus from, SupportTicketStatus to) {
        if (from == to) return;
        if (from == SupportTicketStatus.CLOSED && to != SupportTicketStatus.OPEN) {
            throw new BusinessRuleViolationException(
                    "Closed tickets may only be reopened to OPEN", "SUPPORT-30");
        }
    }

    private SupportTicketMessage saveMessage(String ticketId, String userId, SupportAuthorType type, String body) {
        SupportTicketMessage m = new SupportTicketMessage();
        m.setMessageId(UUID.randomUUID().toString());
        m.setTicketId(ticketId);
        m.setAuthorUserId(userId);
        m.setAuthorType(type);
        m.setMessageBody(body.trim());
        m.setCreatedAt(Instant.now());
        return messageRepository.save(m);
    }

    private void notify(String recipientUserId, NotificationType type, String title, String body,
                        String ticketId, NotificationSeverity severity) {
        if (recipientUserId == null || recipientUserId.isBlank()) return;
        try {
            notificationService.publish(recipientUserId, type, title, body, "SupportTicket", ticketId, severity);
        } catch (Exception ignored) {
            // notifications must not break ticket workflow
        }
    }

    private SupportTicket loadCustomerTicket(String ticketId) {
        String customerId = requireCustomerId();
        return ticketRepository.findByTicketIdAndCustomerId(ticketId, customerId)
                .orElseThrow(() -> ResourceNotFoundException.of("SupportTicket", ticketId));
    }

    private TicketSummary toSummary(SupportTicket t) {
        return new TicketSummary(
                t.getTicketId(), t.getTicketNumber(), t.getSubject(),
                t.getCategory(), t.getPriority(), t.getStatus(), t.getAssignedToUserId(),
                t.getCreatedAt(), t.getUpdatedAt());
    }

    private PlatformTicketSummary toPlatformSummary(SupportTicket t) {
        return new PlatformTicketSummary(
                t.getTicketId(), t.getTicketNumber(), t.getCustomerId(),
                t.getSubject(), t.getCategory(), t.getPriority(), t.getStatus(),
                t.getAssignedToUserId(), t.getCreatedAt(), t.getUpdatedAt());
    }

    private TicketDetail toDetail(SupportTicket t, boolean includeInternal) {
        List<MessageResponse> messages = messageRepository.findByTicketIdOrderByCreatedAtAsc(t.getTicketId())
                .stream()
                .map(m -> new MessageResponse(m.getMessageId(), m.getAuthorUserId(), m.getAuthorType(),
                        m.getMessageBody(), m.getCreatedAt()))
                .toList();
        List<InternalNoteResponse> notes = includeInternal
                ? noteRepository.findByTicketIdOrderByCreatedAtAsc(t.getTicketId()).stream()
                .map(n -> new InternalNoteResponse(n.getNoteId(), n.getAuthorUserId(),
                        n.getNoteBody(), n.getCreatedAt()))
                .toList()
                : List.of();
        return new TicketDetail(
                t.getTicketId(), t.getTicketNumber(), t.getCustomerId(),
                t.getSubject(), t.getDescription(),
                t.getCategory(), t.getPriority(), t.getStatus(),
                t.getCreatedByUserId(), t.getAssignedToUserId(),
                t.getCreatedAt(), t.getUpdatedAt(), t.getResolvedAt(), t.getClosedAt(),
                messages, notes);
    }
}
