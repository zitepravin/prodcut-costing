package com.automotive.costing.platform.user.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.DuplicateResourceException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.platform.user.dto.AdminDtos.*;
import com.automotive.costing.platform.user.entity.Role;
import com.automotive.costing.platform.user.entity.User;
import com.automotive.costing.platform.user.repository.RoleRepository;
import com.automotive.costing.platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * SEC-01 (user provisioning) and SEC-08 (immediate deactivation + session revocation).
 * Role CRUD itself lives in {@link RoleAdminService} - kept separate since a role is
 * reference data shared across users, not something that belongs conceptually inside
 * user management, even though both are behind the same {@code AdminController}.
 */
@Service
@RequiredArgsConstructor
public class UserAdminService {

    private static final String ENTITY_TYPE = "User";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final AuditService auditService;

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("A user with username '" + request.username() + "' already exists.");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("A user with email '" + request.email() + "' already exists.");
        }
        passwordPolicyValidator.validate(request.password());

        Set<Role> roles = resolveRoles(request.roleCodes());

        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus("Active");
        user.setMfaEnabled(false);
        user.setTokenVersion(0);
        user.setFailedLoginAttempts(0);
        user.setPasswordUpdatedAt(Instant.now());
        user.setRoles(roles);

        User saved = userRepository.save(user);
        auditService.recordCreate(ENTITY_TYPE, saved.getUserId(), toResponse(saved));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getById(String userId) {
        return toResponse(getOrThrow(userId));
    }

    /** SEC-01: administrator-managed role assignment, replacing the full set. */
    @Transactional
    public UserResponse updateRoles(String userId, UpdateRolesRequest request) {
        User user = getOrThrow(userId);
        UserResponse before = toResponse(user);
        user.setRoles(resolveRoles(request.roleCodes()));
        User saved = userRepository.save(user);
        auditService.recordUpdate(ENTITY_TYPE, userId, before, toResponse(saved));
        return toResponse(saved);
    }

    /**
     * SEC-08: "allow an administrator to deactivate a user account immediately, revoking
     * all active sessions." status=Inactive blocks future logins (User.isEnabled() is
     * already wired to status); bumping tokenVersion invalidates every token already
     * issued, so this is genuinely immediate - not "revoked next time they'd naturally
     * need to refresh."
     */
    @Transactional
    public UserResponse deactivate(String userId) {
        User user = getOrThrow(userId);
        UserResponse before = toResponse(user);
        user.setStatus("Inactive");
        user.setTokenVersion(user.getTokenVersion() + 1);
        User saved = userRepository.save(user);
        auditService.recordUpdate(ENTITY_TYPE, userId, before, toResponse(saved));
        return toResponse(saved);
    }

    @Transactional
    public UserResponse reactivate(String userId) {
        User user = getOrThrow(userId);
        UserResponse before = toResponse(user);
        user.setStatus("Active");
        User saved = userRepository.save(user);
        auditService.recordUpdate(ENTITY_TYPE, userId, before, toResponse(saved));
        return toResponse(saved);
    }

    private Set<Role> resolveRoles(Set<String> roleCodes) {
        Set<Role> roles = roleCodes.stream()
                .map(code -> roleRepository.findByRoleCode(code)
                        .orElseThrow(() -> ResourceNotFoundException.of("Role", code)))
                .collect(Collectors.toSet());
        if (roles.isEmpty()) {
            throw new BusinessRuleViolationException("At least one valid role is required.", "SEC-01");
        }
        return roles;
    }

    private User getOrThrow(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
    }

    private UserResponse toResponse(User u) {
        Set<String> roleCodes = u.getRoles().stream().map(Role::getRoleCode).collect(Collectors.toSet());
        return new UserResponse(u.getUserId(), u.getUsername(), u.getEmail(), u.getStatus(), u.isMfaEnabled(),
                roleCodes, u.getLastLoginAt());
    }
}
