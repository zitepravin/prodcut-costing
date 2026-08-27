package com.automotive.costing.platform.user.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.platform.user.dto.AdminDtos.GrantScopeRequest;
import com.automotive.costing.platform.user.dto.AdminDtos.ScopeResponse;
import com.automotive.costing.platform.user.entity.ScopeType;
import com.automotive.costing.platform.user.entity.User;
import com.automotive.costing.platform.user.entity.UserAccessScope;
import com.automotive.costing.platform.user.repository.UserAccessScopeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * SEC-03: data segregation by plant, program, and commodity.
 *
 * <p>Semantics: if a user has <em>no</em> {@link UserAccessScope} rows for a given
 * {@link ScopeType}, access is unrestricted on that dimension. If they have one or more
 * rows, access is limited to the granted values only.
 *
 * <p>Call sites: BOM, materials, machines, should-cost, vehicle programs.
 */
@Service
@RequiredArgsConstructor
public class AccessScopeService {

    private final UserAccessScopeRepository scopeRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public boolean hasAccess(User user, ScopeType scopeType, String scopeValue) {
        if (user == null || scopeValue == null) {
            return true;
        }
        var scopes = scopeRepository.findByUserIdAndScopeType(user.getUserId(), scopeType);
        if (scopes.isEmpty()) {
            return true;
        }
        return scopes.stream().anyMatch(s -> s.getScopeValue().equals(scopeValue));
    }

    public void assertAccess(User user, ScopeType scopeType, String scopeValue) {
        if (!hasAccess(user, scopeType, scopeValue)) {
            throw new AccessDeniedException(
                    "User '" + user.getUsername() + "' is not scoped for " + scopeType + " '" + scopeValue + "'.");
        }
    }

    /**
     * Enforces scope for the authenticated principal when present. No-op for system /
     * unauthenticated internal calls (same pattern as BomNodeService).
     */
    public void assertAccessForCurrentUser(ScopeType scopeType, String scopeValue) {
        User user = currentUserOrNull();
        if (user != null) {
            assertAccess(user, scopeType, scopeValue);
        }
    }

    /**
     * @return empty Optional = unrestricted; otherwise the allowed value set for filtering searches
     */
    @Transactional(readOnly = true)
    public Optional<Set<String>> restrictedValues(User user, ScopeType scopeType) {
        if (user == null) {
            return Optional.empty();
        }
        var scopes = scopeRepository.findByUserIdAndScopeType(user.getUserId(), scopeType);
        if (scopes.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(scopes.stream().map(UserAccessScope::getScopeValue).collect(Collectors.toSet()));
    }

    public Optional<Set<String>> restrictedValuesForCurrentUser(ScopeType scopeType) {
        return restrictedValues(currentUserOrNull(), scopeType);
    }

    public User currentUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }

    @Transactional
    public ScopeResponse grantScope(String userId, GrantScopeRequest request) {
        UserAccessScope scope = new UserAccessScope();
        scope.setScopeId(UUID.randomUUID().toString());
        scope.setUserId(userId);
        scope.setScopeType(ScopeType.valueOf(request.scopeType().toUpperCase()));
        scope.setScopeValue(request.scopeValue());
        scope.setCreatedAt(Instant.now());

        UserAccessScope saved = scopeRepository.save(scope);
        auditService.recordCreate("UserAccessScope", saved.getScopeId(), toResponse(saved));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ScopeResponse> listScopes(String userId) {
        return scopeRepository.findByUserId(userId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public void revokeScope(String scopeId) {
        UserAccessScope scope = scopeRepository.findById(scopeId)
                .orElseThrow(() -> ResourceNotFoundException.of("UserAccessScope", scopeId));
        scopeRepository.delete(scope);
        auditService.recordDelete("UserAccessScope", scopeId, toResponse(scope));
    }

    private ScopeResponse toResponse(UserAccessScope s) {
        return new ScopeResponse(s.getScopeId(), s.getUserId(), s.getScopeType().name(), s.getScopeValue());
    }
}
