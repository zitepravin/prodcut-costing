package com.automotive.costing.platform.access.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.platform.access.entity.SupportAccessSession;
import com.automotive.costing.platform.access.repository.PlatformCustomerRepository;
import com.automotive.costing.platform.access.repository.SupportAccessSessionRepository;
import com.automotive.costing.platform.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Time-limited, customer-approved support access. Default READ_ONLY.
 * Does not grant permanent platform access to tenant business data.
 */
@Service
@RequiredArgsConstructor
public class SupportAccessService {

    public record SupportSessionResponse(
            String sessionId, String customerId, String requestedByUserId, String approvedByUserId,
            String status, String accessMode, Instant requestedAt, Instant approvedAt, Instant expiresAt,
            int durationMinutes, String reason
    ) {}

    public record RequestSupportAccess(String customerId, Integer durationMinutes, String reason) {}

    private final SupportAccessSessionRepository sessionRepository;
    private final PlatformCustomerRepository customerRepository;
    private final AuditService auditService;

    @Transactional
    public SupportSessionResponse request(RequestSupportAccess req) {
        customerRepository.findById(req.customerId().trim())
                .orElseThrow(() -> ResourceNotFoundException.of("PlatformCustomer", req.customerId()));
        User user = currentUser();
        SupportAccessSession s = new SupportAccessSession();
        s.setSessionId(UUID.randomUUID().toString());
        s.setCustomerId(req.customerId().trim());
        s.setRequestedByUserId(user.getUserId());
        s.setStatus("PENDING");
        s.setAccessMode("READ_ONLY");
        s.setDurationMinutes(req.durationMinutes() != null && req.durationMinutes() > 0
                ? Math.min(req.durationMinutes(), 240) : 30);
        s.setReason(req.reason());
        s.setRequestedAt(Instant.now());
        sessionRepository.save(s);
        auditService.recordCreate("SupportAccessSession", s.getSessionId(),
                "REQUEST customer=" + s.getCustomerId() + " by=" + user.getUserId());
        return toResponse(s);
    }

    @Transactional
    public SupportSessionResponse approve(String sessionId) {
        SupportAccessSession s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> ResourceNotFoundException.of("SupportAccessSession", sessionId));
        if (!"PENDING".equals(s.getStatus())) {
            throw new BusinessRuleViolationException("Only PENDING sessions can be approved", "SUPPORT-01");
        }
        User user = currentUser();
        s.setStatus("APPROVED");
        s.setApprovedByUserId(user.getUserId());
        s.setApprovedAt(Instant.now());
        s.setExpiresAt(Instant.now().plus(s.getDurationMinutes(), ChronoUnit.MINUTES));
        sessionRepository.save(s);
        auditService.recordTransition("SupportAccessSession", s.getSessionId(), "PENDING", "APPROVED");
        return toResponse(s);
    }

    @Transactional
    public SupportSessionResponse deny(String sessionId) {
        SupportAccessSession s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> ResourceNotFoundException.of("SupportAccessSession", sessionId));
        s.setStatus("DENIED");
        s.setApprovedByUserId(currentUser().getUserId());
        sessionRepository.save(s);
        auditService.recordTransition("SupportAccessSession", s.getSessionId(), "PENDING", "DENIED");
        return toResponse(s);
    }

    @Transactional
    public SupportSessionResponse revoke(String sessionId) {
        SupportAccessSession s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> ResourceNotFoundException.of("SupportAccessSession", sessionId));
        s.setStatus("REVOKED");
        sessionRepository.save(s);
        auditService.recordTransition("SupportAccessSession", s.getSessionId(), s.getStatus(), "REVOKED");
        return toResponse(s);
    }

    @Transactional(readOnly = true)
    public List<SupportSessionResponse> listForCustomer(String customerId) {
        expireIfNeeded(customerId);
        return sessionRepository.findByCustomerIdOrderByRequestedAtDesc(customerId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public boolean hasActiveSupportAccess(String customerId, String userId) {
        Instant now = Instant.now();
        return sessionRepository
                .findFirstByCustomerIdAndRequestedByUserIdAndStatusAndExpiresAtAfter(
                        customerId, userId, "APPROVED", now)
                .isPresent();
    }

    private void expireIfNeeded(String customerId) {
        Instant now = Instant.now();
        for (SupportAccessSession s : sessionRepository.findByCustomerIdOrderByRequestedAtDesc(customerId)) {
            if ("APPROVED".equals(s.getStatus()) && s.getExpiresAt() != null && s.getExpiresAt().isBefore(now)) {
                s.setStatus("EXPIRED");
                sessionRepository.save(s);
                auditService.recordTransition("SupportAccessSession", s.getSessionId(), "APPROVED", "EXPIRED");
            }
        }
    }

    private User currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User u)) {
            throw new BusinessRuleViolationException("Authentication required", "SUPPORT-02");
        }
        return u;
    }

    private SupportSessionResponse toResponse(SupportAccessSession s) {
        return new SupportSessionResponse(
                s.getSessionId(), s.getCustomerId(), s.getRequestedByUserId(), s.getApprovedByUserId(),
                s.getStatus(), s.getAccessMode(), s.getRequestedAt(), s.getApprovedAt(), s.getExpiresAt(),
                s.getDurationMinutes(), s.getReason()
        );
    }
}
