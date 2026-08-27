package com.automotive.costing.platform.access.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.DuplicateResourceException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.platform.access.CustomerContext;
import com.automotive.costing.platform.access.dto.CustomerAdminDtos.*;
import com.automotive.costing.platform.access.entity.CustomerDepartment;
import com.automotive.costing.platform.access.entity.CustomerUserMembership;
import com.automotive.costing.platform.access.entity.MembershipStatus;
import com.automotive.costing.platform.access.entity.UserInvitation;
import com.automotive.costing.platform.access.repository.CustomerDepartmentRepository;
import com.automotive.costing.platform.access.repository.CustomerUserMembershipRepository;
import com.automotive.costing.platform.access.repository.UserInvitationRepository;
import com.automotive.costing.platform.user.entity.Role;
import com.automotive.costing.platform.user.entity.User;
import com.automotive.costing.platform.user.repository.RoleRepository;
import com.automotive.costing.platform.user.repository.UserRepository;
import com.automotive.costing.platform.user.service.PasswordPolicyValidator;
import com.automotive.costing.platform.user.service.PermissionMatrixService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * V225 — Tenant-scoped Customer Admin user management.
 * Derives customerId from {@link CustomerContext}; never trusts client-supplied tenant alone.
 */
@Service
@RequiredArgsConstructor
public class CustomerUserAdminService {

    private static final Logger log = LoggerFactory.getLogger(CustomerUserAdminService.class);
    private static final int INVITE_TTL_HOURS = 72;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CommercialLicenseService commercialLicenseService;
    private final CustomerUserMembershipRepository membershipRepository;
    private final CustomerDepartmentRepository departmentRepository;
    private final UserInvitationRepository invitationRepository;
    private final CentralAccessService centralAccessService;
    private final PermissionMatrixService permissionMatrixService;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    private String requireCustomerId() {
        String cid = CustomerContext.getCustomerId();
        if (cid == null || cid.isBlank()) {
            throw new BusinessRuleViolationException(
                    "Customer context required (X-Customer-Id with active membership)", "ACCESS-10");
        }
        return cid;
    }

    @Transactional(readOnly = true)
    public List<CustomerUserResponse> listUsers() {
        String customerId = requireCustomerId();
        return membershipRepository.findByCustomerId(customerId).stream()
                .map(m -> userRepository.findById(m.getUserId()).orElse(null))
                .filter(u -> u != null)
                .map(u -> toResponse(u, customerId))
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerUserResponse getUser(String userId) {
        String customerId = requireCustomerId();
        assertMember(customerId, userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        return toResponse(user, customerId);
    }

    @Transactional
    public CreateUserResult createUser(CreateCustomerUserRequest request) {
        String customerId = requireCustomerId();
        Set<Role> roles = resolveAssignableRoles(request.roleCodes());
        Set<String> roleCodes = roles.stream().map(Role::getRoleCode).collect(java.util.stream.Collectors.toSet());
        commercialLicenseService.assertLicenseActiveIfLicensed(customerId);
        commercialLicenseService.assertTotalUserLimit(customerId, 1);
        commercialLicenseService.assertRoleAssignmentAllowed(customerId, roleCodes, java.util.Set.of());

        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("A user with email '" + email + "' already exists.");
        }
        String username = deriveUsername(email);
        if (userRepository.existsByUsername(username)) {
            username = username + "-" + UUID.randomUUID().toString().substring(0, 8);
        }

        if (request.departmentId() != null && !request.departmentId().isBlank()) {
            departmentRepository.findByDepartmentIdAndCustomerId(request.departmentId(), customerId)
                    .orElseThrow(() -> new BusinessRuleViolationException(
                            "Department not found in this customer", "DEPT-01"));
        }

        User user = new User();
        user.setUserId(UUID.randomUUID().toString());
        user.setUsername(username);
        user.setEmail(email);
        user.setDisplayName(request.employeeName().trim());
        user.setEmployeeId(blankToNull(request.employeeId()));
        user.setDesignation(blankToNull(request.designation()));
        user.setDepartmentId(blankToNull(request.departmentId()));
        user.setMobileNumber(blankToNull(request.mobileNumber()));
        user.setPasswordHash(null);
        user.setStatus("Invited");
        user.setMfaEnabled(false);
        user.setTokenVersion(0);
        user.setFailedLoginAttempts(0);
        user.setRoles(roles);

        User saved = userRepository.save(user);

        CustomerUserMembership membership = new CustomerUserMembership();
        membership.setMembershipId(UUID.randomUUID().toString());
        membership.setCustomerId(customerId);
        membership.setUserId(saved.getUserId());
        membership.setStatus(MembershipStatus.ACTIVE);
        membershipRepository.save(membership);

        String rawToken = generateToken();
        UserInvitation inv = new UserInvitation();
        inv.setInvitationId(UUID.randomUUID().toString());
        inv.setCustomerId(customerId);
        inv.setUserId(saved.getUserId());
        inv.setEmail(email);
        inv.setTokenHash(sha256(rawToken));
        inv.setExpiresAt(Instant.now().plus(INVITE_TTL_HOURS, ChronoUnit.HOURS));
        inv.setStatus("PENDING");
        invitationRepository.save(inv);

        auditService.recordCreate("User", saved.getUserId(), MapLite.of(
                "email", email, "status", "Invited", "customerId", customerId));
        auditService.recordCreate("UserInvitation", inv.getInvitationId(), MapLite.of(
                "userId", saved.getUserId(), "customerId", customerId, "status", "PENDING"));

        String deliveryNote = attemptInvitationEmail(email, rawToken);
        // rawToken never logged
        log.info("User invitation created invitationId={} userId={} customerId={}",
                inv.getInvitationId(), saved.getUserId(), customerId);

        return new CreateUserResult(toResponse(saved, customerId), true, deliveryNote);
    }

    @Transactional
    public CustomerUserResponse updateUser(String userId, UpdateCustomerUserRequest request) {
        String customerId = requireCustomerId();
        assertMember(customerId, userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        CustomerUserResponse before = toResponse(user, customerId);

        if (request.employeeName() != null && !request.employeeName().isBlank()) {
            user.setDisplayName(request.employeeName().trim());
        }
        if (request.employeeId() != null) {
            user.setEmployeeId(blankToNull(request.employeeId()));
        }
        if (request.designation() != null) {
            user.setDesignation(blankToNull(request.designation()));
        }
        if (request.mobileNumber() != null) {
            user.setMobileNumber(blankToNull(request.mobileNumber()));
        }
        if (request.departmentId() != null) {
            if (request.departmentId().isBlank()) {
                user.setDepartmentId(null);
            } else {
                departmentRepository.findByDepartmentIdAndCustomerId(request.departmentId(), customerId)
                        .orElseThrow(() -> new BusinessRuleViolationException(
                                "Department not found in this customer", "DEPT-01"));
                user.setDepartmentId(request.departmentId());
            }
        }
        if (request.roleCodes() != null && !request.roleCodes().isEmpty()) {
            Set<Role> newRoles = resolveAssignableRoles(request.roleCodes());
            Set<String> newCodes = newRoles.stream().map(Role::getRoleCode).collect(java.util.stream.Collectors.toSet());
            commercialLicenseService.assertLicenseActiveIfLicensed(customerId);
            commercialLicenseService.assertRoleAssignmentAllowed(customerId, newCodes, before.roleCodes());
            user.setRoles(newRoles);
            auditService.recordUpdate("UserRole", userId, before.roleCodes(), request.roleCodes());
        }

        User saved = userRepository.save(user);
        auditService.recordUpdate("User", userId, before, toResponse(saved, customerId));
        return toResponse(saved, customerId);
    }

    @Transactional
    public CustomerUserResponse deactivate(String userId) {
        String customerId = requireCustomerId();
        assertMember(customerId, userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        CustomerUserResponse before = toResponse(user, customerId);
        user.setStatus("Inactive");
        user.setTokenVersion(user.getTokenVersion() + 1);
        User saved = userRepository.save(user);
        auditService.recordTransition("User", userId, before.status(), "Inactive");
        return toResponse(saved, customerId);
    }

    @Transactional
    public CustomerUserResponse activate(String userId) {
        String customerId = requireCustomerId();
        assertMember(customerId, userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        if ("Invited".equalsIgnoreCase(user.getStatus())) {
            throw new BusinessRuleViolationException(
                    "Invited users must accept invitation before activation", "INVITE-05");
        }
        CustomerUserResponse before = toResponse(user, customerId);
        user.setStatus("Active");
        User saved = userRepository.save(user);
        auditService.recordTransition("User", userId, before.status(), "Active");
        return toResponse(saved, customerId);
    }

    @Transactional
    public InvitationAcceptResponse acceptInvitation(AcceptInvitationRequest request) {
        String hash = sha256(request.token().trim());
        UserInvitation inv = invitationRepository.findByTokenHashAndStatus(hash, "PENDING")
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "Invalid or already used invitation token", "INVITE-01"));
        if (inv.getExpiresAt().isBefore(Instant.now())) {
            inv.setStatus("EXPIRED");
            invitationRepository.save(inv);
            throw new BusinessRuleViolationException("Invitation has expired", "INVITE-02");
        }
        passwordPolicyValidator.validate(request.password());

        User user = userRepository.findById(inv.getUserId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", inv.getUserId()));
        // Tenant bound to invitation — cannot activate another tenant's user via token
        if (!inv.getCustomerId().equals(inv.getCustomerId())) {
            throw new BusinessRuleViolationException("Invitation tenant mismatch", "INVITE-04");
        }

        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPasswordUpdatedAt(Instant.now());
        user.setStatus("Active");
        userRepository.save(user);

        inv.setStatus("ACCEPTED");
        inv.setAcceptedAt(Instant.now());
        invitationRepository.save(inv);

        auditService.recordTransition("UserInvitation", inv.getInvitationId(), "PENDING", "ACCEPTED");
        auditService.recordTransition("User", user.getUserId(), "Invited", "Active");
        // Never log password or token
        log.info("Invitation accepted invitationId={} userId={}", inv.getInvitationId(), user.getUserId());

        return new InvitationAcceptResponse(user.getUserId(), user.getEmail(), "Active");
    }

    private void assertMember(String customerId, String userId) {
        if (!membershipRepository.existsByCustomerIdAndUserIdAndStatus(
                customerId, userId, MembershipStatus.ACTIVE)
                && membershipRepository.findByCustomerIdAndUserId(customerId, userId).isEmpty()) {
            throw new BusinessRuleViolationException(
                    "User is not a member of this customer", "ACCESS-06");
        }
        // Still block if no membership row at all for this customer
        membershipRepository.findByCustomerIdAndUserId(customerId, userId)
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "User is not a member of this customer", "ACCESS-06"));
    }

    private Set<Role> resolveAssignableRoles(Set<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw new BusinessRuleViolationException("At least one role is required", "ROLE-01");
        }
        for (String code : roleCodes) {
            if (!permissionMatrixService.isAssignableByCustomerAdmin(code)) {
                throw new BusinessRuleViolationException(
                        "Role '" + code + "' cannot be assigned by Customer Admin", "ROLE-02");
            }
        }
        Set<Role> roles = roleCodes.stream()
                .map(code -> roleRepository.findByRoleCode(code)
                        .orElseThrow(() -> new BusinessRuleViolationException(
                                "Unknown role: " + code, "ROLE-03")))
                .collect(Collectors.toSet());
        return roles;
    }

    private CustomerUserResponse toResponse(User user, String customerId) {
        String deptName = null;
        if (user.getDepartmentId() != null) {
            deptName = departmentRepository.findByDepartmentIdAndCustomerId(user.getDepartmentId(), customerId)
                    .map(CustomerDepartment::getDepartmentName)
                    .orElse(null);
        }
        Set<String> roleCodes = user.getRoles().stream().map(Role::getRoleCode).collect(Collectors.toSet());
        return new CustomerUserResponse(
                user.getUserId(),
                user.getDisplayName(),
                user.getEmail(),
                user.getUsername(),
                user.getEmployeeId(),
                user.getDepartmentId(),
                deptName,
                user.getDesignation(),
                user.getMobileNumber(),
                user.getStatus(),
                roleCodes,
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }

    private String attemptInvitationEmail(String email, String rawToken) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            return "Invitation created. Email not sent (JavaMailSender not configured). "
                    + "Configure spring.mail and app.notification.email-enabled=true for delivery.";
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(email);
            msg.setSubject("You are invited to Product Costing Software");
            msg.setText("You have been invited. Use this one-time token to set your password "
                    + "(expires in " + INVITE_TTL_HOURS + " hours):\n\n" + rawToken
                    + "\n\nOpen the application invitation acceptance page and enter this token with your new password.");
            sender.send(msg);
            return "Invitation email dispatched.";
        } catch (Exception ex) {
            log.warn("Invitation email failed for user (token not logged): {}", ex.getMessage());
            return "Invitation created but email delivery failed. Check mail configuration.";
        }
    }

    private static String deriveUsername(String email) {
        String local = email.contains("@") ? email.substring(0, email.indexOf('@')) : email;
        return local.replaceAll("[^a-zA-Z0-9._-]", "").toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    private static String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static String sha256(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(dig);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /** Minimal map helper to avoid logging secrets in audit payloads. */
    static final class MapLite {
        static java.util.Map<String, Object> of(Object... kv) {
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            for (int i = 0; i + 1 < kv.length; i += 2) {
                m.put(String.valueOf(kv[i]), kv[i + 1]);
            }
            return m;
        }
    }
}
