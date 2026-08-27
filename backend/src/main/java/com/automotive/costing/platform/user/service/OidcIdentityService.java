package com.automotive.costing.platform.user.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.config.OidcProperties;
import com.automotive.costing.platform.user.dto.OidcDtos.LinkIdentityRequest;
import com.automotive.costing.platform.user.dto.OidcDtos.OidcLinkResponse;
import com.automotive.costing.platform.user.entity.OidcIdentityLink;
import com.automotive.costing.platform.user.entity.OidcLinkStatus;
import com.automotive.costing.platform.user.entity.User;
import com.automotive.costing.platform.user.repository.OidcIdentityLinkRepository;
import com.automotive.costing.platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * SEC-02 identity linking for OIDC. Does not perform browser redirect login —
 * that requires oauth2-client on a deployment with {@code app.security.oidc.enabled=true}.
 */
@Service
@RequiredArgsConstructor
public class OidcIdentityService {

    private final OidcIdentityLinkRepository linkRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final OidcProperties oidcProperties;

    @Transactional
    public OidcLinkResponse link(LinkIdentityRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", request.userId()));
        if (linkRepository.existsByIssuerAndSubjectAndStatus(
                request.issuer(), request.subject(), OidcLinkStatus.ACTIVE)) {
            throw new BusinessRuleViolationException(
                    "Issuer/subject already linked to a user", "SEC-02-link-exists");
        }
        OidcIdentityLink link = new OidcIdentityLink();
        link.setLinkId(UUID.randomUUID().toString());
        link.setUserId(user.getUserId());
        link.setIssuer(request.issuer().trim());
        link.setSubject(request.subject().trim());
        link.setEmail(request.email());
        link.setLinkedAt(Instant.now());
        link.setStatus(OidcLinkStatus.ACTIVE);
        linkRepository.save(link);

        // Keep primary sso_subject in sync when empty or matching this issuer
        if (user.getSsoSubject() == null || user.getSsoSubject().isBlank()) {
            user.setSsoSubject(request.subject().trim());
            userRepository.save(user);
        }
        auditService.recordCreate("OidcIdentityLink", link.getLinkId(),
                link.getIssuer() + "|" + link.getSubject());
        return toResponse(link);
    }

    @Transactional
    public OidcLinkResponse revoke(String linkId) {
        OidcIdentityLink link = linkRepository.findById(linkId)
                .orElseThrow(() -> ResourceNotFoundException.of("OidcIdentityLink", linkId));
        link.setStatus(OidcLinkStatus.REVOKED);
        linkRepository.save(link);
        auditService.recordUpdate("OidcIdentityLink", linkId, null, "REVOKED");
        return toResponse(link);
    }

    @Transactional(readOnly = true)
    public List<OidcLinkResponse> listForUser(String userId) {
        return linkRepository.findByUserIdAndStatusOrderByLinkedAtDesc(userId, OidcLinkStatus.ACTIVE)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OidcLinkResponse resolve(String issuer, String subject) {
        OidcIdentityLink link = linkRepository
                .findByIssuerAndSubjectAndStatus(issuer, subject, OidcLinkStatus.ACTIVE)
                .orElseThrow(() -> ResourceNotFoundException.of("OidcIdentityLink", issuer + "|" + subject));
        return toResponse(link);
    }

    /**
     * Called by future OAuth2 success handler when OIDC is enabled.
     */
    @Transactional
    public OidcLinkResponse recordLogin(String issuer, String subject) {
        OidcIdentityLink link = linkRepository
                .findByIssuerAndSubjectAndStatus(issuer, subject, OidcLinkStatus.ACTIVE)
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "No linked user for this IdP identity. Admin must link first.", "SEC-02-unlinked"));
        link.setLastLoginAt(Instant.now());
        linkRepository.save(link);
        return toResponse(link);
    }

    public boolean isOidcEnabled() {
        return oidcProperties.isEnabled();
    }

    private OidcLinkResponse toResponse(OidcIdentityLink link) {
        return new OidcLinkResponse(
                link.getLinkId(),
                link.getUserId(),
                link.getIssuer(),
                link.getSubject(),
                link.getEmail(),
                link.getStatus().name(),
                link.getLinkedAt(),
                link.getLastLoginAt());
    }
}
