package com.automotive.costing.rfq.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.notification.entity.NotificationSeverity;
import com.automotive.costing.notification.entity.NotificationType;
import com.automotive.costing.notification.service.NotificationService;
import com.automotive.costing.platform.user.entity.User;
import com.automotive.costing.platform.user.repository.UserRepository;
import com.automotive.costing.rfq.entity.PortalEventLog;
import com.automotive.costing.rfq.repository.PortalEventLogRepository;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.config.JwtProperties;
import com.automotive.costing.rfq.dto.PortalDtos.AcceptInviteRequest;
import com.automotive.costing.rfq.dto.PortalDtos.CreateInviteRequest;
import com.automotive.costing.rfq.dto.PortalDtos.InviteResponse;
import com.automotive.costing.rfq.dto.PortalDtos.PortalLoginRequest;
import com.automotive.costing.rfq.dto.PortalDtos.PortalQuoteSubmitRequest;
import com.automotive.costing.rfq.dto.PortalDtos.PortalTokenResponse;
import com.automotive.costing.rfq.dto.RfqDtos.QuoteResponse;
import com.automotive.costing.rfq.dto.RfqDtos.SubmitQuoteRequest;
import com.automotive.costing.rfq.entity.PortalAccountStatus;
import com.automotive.costing.rfq.entity.PortalInviteStatus;
import com.automotive.costing.rfq.entity.SupplierPortalAccount;
import com.automotive.costing.rfq.entity.SupplierPortalInvite;
import com.automotive.costing.rfq.entity.SupplierStatus;
import com.automotive.costing.rfq.repository.SupplierPortalAccountRepository;
import com.automotive.costing.rfq.repository.SupplierPortalInviteRepository;
import com.automotive.costing.rfq.repository.SupplierPortalRfqAccessRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.rfq.repository.RfqEventRepository;
import com.automotive.costing.rfq.repository.RfqClarificationRequestRepository;
import com.automotive.costing.rfq.repository.RfqAttachmentRepository;
import com.automotive.costing.rfq.entity.RfqClarificationRequest;
import com.automotive.costing.rfq.entity.RfqAttachment;
import com.automotive.costing.rfq.dto.PortalDtos.CreateClarificationRequest;
import com.automotive.costing.rfq.dto.PortalDtos.ClarificationResponse;
import com.automotive.costing.rfq.dto.PortalDtos.RespondClarificationRequest;
import com.automotive.costing.rfq.dto.PortalDtos.CreateAttachmentRequest;
import com.automotive.costing.rfq.dto.PortalDtos.AttachmentResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.automotive.costing.rfq.entity.SupplierPortalRfqAccess;
import com.automotive.costing.rfq.entity.PortalRfqAccessStatus;
import com.automotive.costing.rfq.dto.PortalDtos.GrantRfqAccessRequest;
import com.automotive.costing.rfq.dto.PortalDtos.PortalRfqAccessResponse;
import com.automotive.costing.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupplierPortalService {

    private static final int INVITE_TTL_DAYS = 7;

    private final SupplierPortalAccountRepository accountRepository;
    private final SupplierPortalInviteRepository inviteRepository;
    private final SupplierPortalRfqAccessRepository rfqAccessRepository;
    private final RfqEventRepository rfqEventRepository;
    private final TenantAccessGuard tenantAccessGuard;
    private final RfqService rfqService;
    private final SupplierMasterService supplierMasterService;
    private final SupplierQuoteService supplierQuoteService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final AuditService auditService;
    private final PortalEventLogRepository portalEventLogRepository;
    private final RfqClarificationRequestRepository clarificationRepository;
    private final RfqAttachmentRepository attachmentRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @Transactional
    public InviteResponse createInvite(CreateInviteRequest request) {
        var supplier = supplierMasterService.require(request.supplierId());
        if (supplier.getStatus() != SupplierStatus.ACTIVE) {
            throw new BusinessRuleViolationException("Supplier must be ACTIVE to invite to portal", "PORTAL-01");
        }
        if (accountRepository.existsByEmailIgnoreCase(request.email())) {
            throw new BusinessRuleViolationException("Portal account already exists for this email", "PORTAL-02");
        }
        String rawToken = randomToken();
        SupplierPortalInvite invite = new SupplierPortalInvite();
        invite.setInviteId(UUID.randomUUID().toString());
        invite.setSupplierId(request.supplierId());
        invite.setEmail(request.email().trim().toLowerCase());
        invite.setTokenHash(sha256(rawToken));
        invite.setExpiresAt(Instant.now().plus(INVITE_TTL_DAYS, ChronoUnit.DAYS));
        invite.setStatus(PortalInviteStatus.PENDING);
        inviteRepository.save(invite);
        auditService.recordCreate("SupplierPortalInvite", invite.getInviteId(), invite.getEmail());
        logPortalEvent("PORTAL_INVITE_CREATED", invite.getSupplierId(), null, null, invite.getEmail(),
                "Invite " + invite.getInviteId());
        notifyBuyers(NotificationType.PORTAL_INVITE_CREATED,
                "Portal invite created",
                "Supplier invite sent to " + invite.getEmail(),
                "SupplierPortalInvite", invite.getInviteId());
        return new InviteResponse(invite.getInviteId(), invite.getSupplierId(), invite.getEmail(),
                invite.getExpiresAt(), rawToken);
    }

    @Transactional
    public PortalTokenResponse acceptInvite(AcceptInviteRequest request) {
        SupplierPortalInvite invite = inviteRepository.findByTokenHash(sha256(request.inviteToken()))
                .orElseThrow(() -> new BusinessRuleViolationException("Invalid invite token", "PORTAL-03"));
        if (invite.getStatus() != PortalInviteStatus.PENDING) {
            throw new BusinessRuleViolationException("Invite is not pending", "PORTAL-04");
        }
        if (invite.getExpiresAt().isBefore(Instant.now())) {
            invite.setStatus(PortalInviteStatus.EXPIRED);
            inviteRepository.save(invite);
            throw new BusinessRuleViolationException("Invite has expired", "PORTAL-05");
        }
        if (accountRepository.existsByEmailIgnoreCase(invite.getEmail())) {
            throw new BusinessRuleViolationException("Portal account already exists", "PORTAL-02");
        }
        SupplierPortalAccount account = new SupplierPortalAccount();
        account.setPortalAccountId(UUID.randomUUID().toString());
        account.setSupplierId(invite.getSupplierId());
        account.setEmail(invite.getEmail());
        account.setPasswordHash(passwordEncoder.encode(request.password()));
        account.setStatus(PortalAccountStatus.ACTIVE);
        accountRepository.save(account);

        invite.setStatus(PortalInviteStatus.ACCEPTED);
        invite.setAcceptedAt(Instant.now());
        inviteRepository.save(invite);
        auditService.recordCreate("SupplierPortalAccount", account.getPortalAccountId(), account.getEmail());

        String token = jwtTokenProvider.generatePortalToken(
                account.getEmail(), account.getSupplierId(), account.getPortalAccountId());
        return new PortalTokenResponse(token, "Bearer", jwtProperties.getAccessTokenExpirationMs(),
                account.getSupplierId(), account.getEmail());
    }

    @Transactional
    public PortalTokenResponse login(PortalLoginRequest request) {
        SupplierPortalAccount account = accountRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new BusinessRuleViolationException("Invalid portal credentials", "PORTAL-06"));
        if (account.getStatus() != PortalAccountStatus.ACTIVE) {
            throw new BusinessRuleViolationException("Portal account disabled", "PORTAL-07");
        }
        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new BusinessRuleViolationException("Invalid portal credentials", "PORTAL-06");
        }
        account.setLastLoginAt(Instant.now());
        accountRepository.save(account);
        String token = jwtTokenProvider.generatePortalToken(
                account.getEmail(), account.getSupplierId(), account.getPortalAccountId());
        return new PortalTokenResponse(token, "Bearer", jwtProperties.getAccessTokenExpirationMs(),
                account.getSupplierId(), account.getEmail());
    }

    /**
     * Portal quote submit — supplierId forced from portal token (cannot spoof other suppliers).
     */
    @Transactional
    public QuoteResponse submitQuote(String supplierIdFromToken, PortalQuoteSubmitRequest request) {
        assertPortalRfqAccess(supplierIdFromToken, request.rfqId());
        var quote = supplierQuoteService.submit(request.rfqId(), new SubmitQuoteRequest(
                request.rfqLineId(),
                supplierIdFromToken,
                request.unitPrice(),
                request.currencyCode(),
                request.toolingLumpSum(),
                request.leadTimeDays(),
                request.validFrom(),
                request.validTo(),
                null,
                request.notes()
        ));
        logPortalEvent("PORTAL_QUOTE_SUBMITTED", supplierIdFromToken, request.rfqId(),
                quote.quoteId(), null, "Unit price " + request.unitPrice());
        notifyBuyers(NotificationType.PORTAL_QUOTE_SUBMITTED,
                "Portal quote submitted",
                "Supplier " + supplierIdFromToken + " submitted a quote on RFQ " + request.rfqId(),
                "SupplierQuote", quote.quoteId());
        return quote;
    }

    
    @Transactional
    public PortalRfqAccessResponse grantRfqAccess(GrantRfqAccessRequest request) {
        requireOwnedRfq(request.rfqId());
        supplierMasterService.require(request.supplierId());
        rfqService.require(request.rfqId());
        if (rfqAccessRepository.existsBySupplierIdAndRfqIdAndStatus(
                request.supplierId(), request.rfqId(), PortalRfqAccessStatus.ACTIVE)) {
            throw new BusinessRuleViolationException("Supplier already has access to this RFQ", "PORTAL-10");
        }
        SupplierPortalRfqAccess access = new SupplierPortalRfqAccess();
        access.setAccessId(UUID.randomUUID().toString());
        access.setSupplierId(request.supplierId());
        access.setRfqId(request.rfqId());
        access.setGrantedAt(Instant.now());
        access.setStatus(PortalRfqAccessStatus.ACTIVE);
        rfqAccessRepository.save(access);
        auditService.recordCreate("SupplierPortalRfqAccess", access.getAccessId(),
                access.getSupplierId() + ":" + access.getRfqId());
        logPortalEvent("PORTAL_RFQ_ACCESS_GRANTED", access.getSupplierId(), access.getRfqId(), null, null,
                "Access " + access.getAccessId());
        notifyBuyers(NotificationType.PORTAL_RFQ_ACCESS_GRANTED,
                "Portal RFQ access granted",
                "Supplier " + access.getSupplierId() + " granted access to RFQ " + access.getRfqId(),
                "RfqEvent", access.getRfqId());
        return new PortalRfqAccessResponse(access.getAccessId(), access.getSupplierId(),
                access.getRfqId(), access.getStatus().name(), access.getGrantedAt());
    }

    @Transactional
    public PortalRfqAccessResponse revokeRfqAccess(String accessId) {
        SupplierPortalRfqAccess access = rfqAccessRepository.findById(accessId)
                .orElseThrow(() -> ResourceNotFoundException.of("SupplierPortalRfqAccess", accessId));
        requireOwnedRfq(access.getRfqId());
        access.setStatus(PortalRfqAccessStatus.REVOKED);
        access.setRevokedAt(Instant.now());
        rfqAccessRepository.save(access);
        auditService.recordUpdate("SupplierPortalRfqAccess", accessId, null, "REVOKED");
        return new PortalRfqAccessResponse(access.getAccessId(), access.getSupplierId(),
                access.getRfqId(), access.getStatus().name(), access.getGrantedAt());
    }

    @Transactional(readOnly = true)
    public List<PortalRfqAccessResponse> listActiveAccessForSupplier(String supplierId) {
        return rfqAccessRepository.findBySupplierIdAndStatusOrderByGrantedAtDesc(
                        supplierId, PortalRfqAccessStatus.ACTIVE).stream()
                .map(a -> new PortalRfqAccessResponse(a.getAccessId(), a.getSupplierId(), a.getRfqId(),
                        a.getStatus().name(), a.getGrantedAt()))
                .toList();
    }


    private void requireOwnedRfq(String rfqId) {
        String customerId = tenantAccessGuard.requireCustomerId();
        var rfq = rfqEventRepository.findByCustomerIdAndRfqId(customerId, rfqId)
                .orElseThrow(() -> ResourceNotFoundException.of("RfqEvent", rfqId));
        tenantAccessGuard.assertOwns(rfq.getCustomerId(), "RfqEvent", rfqId);
    }

    public void assertPortalRfqAccess(String supplierId, String rfqId) {
        if (!rfqAccessRepository.existsBySupplierIdAndRfqIdAndStatus(
                supplierId, rfqId, PortalRfqAccessStatus.ACTIVE)) {
            throw new BusinessRuleViolationException(
                    "Supplier is not granted portal access to this RFQ", "PORTAL-11");
        }
    }

    public String requireSupplierIdFromPortalToken(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new BusinessRuleViolationException("Portal bearer token required", "PORTAL-08");
        }
        String token = bearerToken.substring(7);
        if (!jwtTokenProvider.isPortalTokenValid(token)) {
            throw new BusinessRuleViolationException("Invalid or expired portal token", "PORTAL-09");
        }
        String supplierId = jwtTokenProvider.extractSupplierId(token);
        if (supplierId == null || supplierId.isBlank()) {
            throw new BusinessRuleViolationException("Portal token missing supplier", "PORTAL-09");
        }
        return supplierId;
    }


    @Transactional
    public ClarificationResponse createClarification(String supplierIdFromToken, CreateClarificationRequest request) {
        assertPortalRfqAccess(supplierIdFromToken, request.rfqId());
        RfqClarificationRequest c = new RfqClarificationRequest();
        c.setClarificationId(UUID.randomUUID().toString());
        c.setRfqId(request.rfqId());
        c.setSupplierId(supplierIdFromToken);
        c.setQuestionText(request.questionText().trim());
        c.setStatus("OPEN");
        clarificationRepository.save(c);
        logPortalEvent("PORTAL_CLARIFICATION_CREATED", supplierIdFromToken, request.rfqId(), null, null,
                request.questionText().substring(0, Math.min(200, request.questionText().length())));
        notifyBuyers(NotificationType.PORTAL_QUOTE_SUBMITTED,
                "Seller clarification requested",
                "Supplier " + supplierIdFromToken + " asked a question on RFQ " + request.rfqId(),
                "RfqClarification", c.getClarificationId());
        return toClarification(c);
    }

    @Transactional(readOnly = true)
    public List<ClarificationResponse> listClarificationsForSupplier(String supplierIdFromToken, String rfqId) {
        assertPortalRfqAccess(supplierIdFromToken, rfqId);
        return clarificationRepository.findByRfqIdAndSupplierIdOrderByCreatedAtDesc(rfqId, supplierIdFromToken)
                .stream().map(this::toClarification).toList();
    }

    @Transactional(readOnly = true)
    public List<ClarificationResponse> listClarificationsForBuyer(String rfqId) {
        rfqService.getDetail(rfqId);
        return clarificationRepository.findByRfqIdOrderByCreatedAtDesc(rfqId).stream()
                .map(this::toClarification).toList();
    }

    @Transactional
    public ClarificationResponse respondClarification(String clarificationId, RespondClarificationRequest request) {
        RfqClarificationRequest c = clarificationRepository.findById(clarificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Clarification", clarificationId));
        rfqService.getDetail(c.getRfqId());
        if (!"OPEN".equals(c.getStatus())) {
            throw new BusinessRuleViolationException("Clarification is not OPEN", "PORTAL-20");
        }
        c.setBuyerResponse(request.buyerResponse().trim());
        c.setStatus("ANSWERED");
        c.setRespondedAt(Instant.now());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        c.setRespondedBy(auth != null ? auth.getName() : null);
        clarificationRepository.save(c);
        return toClarification(c);
    }

    @Transactional
    public AttachmentResponse addAttachment(CreateAttachmentRequest request) {
        rfqService.getDetail(request.rfqId());
        RfqAttachment a = new RfqAttachment();
        a.setAttachmentId(UUID.randomUUID().toString());
        a.setRfqId(request.rfqId());
        a.setFileName(request.fileName().trim());
        a.setContentType(request.contentType());
        a.setStorageUri(request.storageUri().trim());
        a.setNotes(request.notes());
        a.setUploadedAt(Instant.now());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        a.setUploadedBy(auth != null ? auth.getName() : null);
        attachmentRepository.save(a);
        return toAttachment(a);
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> listAttachments(String rfqId) {
        rfqService.getDetail(rfqId);
        return attachmentRepository.findByRfqIdOrderByUploadedAtDesc(rfqId).stream()
                .map(this::toAttachment).toList();
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> listAttachmentsForPortal(String supplierIdFromToken, String rfqId) {
        assertPortalRfqAccess(supplierIdFromToken, rfqId);
        return attachmentRepository.findByRfqIdOrderByUploadedAtDesc(rfqId).stream()
                .map(this::toAttachment).toList();
    }

    private ClarificationResponse toClarification(RfqClarificationRequest c) {
        return new ClarificationResponse(
                c.getClarificationId(), c.getRfqId(), c.getSupplierId(), c.getQuestionText(),
                c.getStatus(), c.getBuyerResponse(), c.getRespondedAt(), c.getCreatedAt());
    }

    private AttachmentResponse toAttachment(RfqAttachment a) {
        return new AttachmentResponse(
                a.getAttachmentId(), a.getRfqId(), a.getFileName(), a.getContentType(),
                a.getStorageUri(), a.getUploadedBy(), a.getUploadedAt(), a.getNotes());
    }


    private void logPortalEvent(String type, String supplierId, String rfqId, String quoteId,
                                String actorEmail, String detail) {
        PortalEventLog e = new PortalEventLog();
        e.setEventId(UUID.randomUUID().toString());
        e.setEventType(type);
        e.setSupplierId(supplierId);
        e.setRfqId(rfqId);
        e.setQuoteId(quoteId);
        e.setActorEmail(actorEmail);
        e.setDetail(detail);
        e.setCreatedAt(Instant.now());
        portalEventLogRepository.save(e);
    }

    private void notifyBuyers(NotificationType type, String title, String body,
                              String entityType, String entityId) {
        for (String role : List.of("PURCHASING_BUYER", "SOURCING_LEAD", "COSTING_MANAGER")) {
            for (User u : userRepository.findActiveByRoleCode(role)) {
                notificationService.publish(u.getUserId(), type, title, body, entityType, entityId,
                        NotificationSeverity.INFO);
            }
        }
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static String sha256(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}