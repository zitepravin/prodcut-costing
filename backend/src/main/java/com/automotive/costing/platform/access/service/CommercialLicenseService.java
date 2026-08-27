package com.automotive.costing.platform.access.service;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.platform.access.CustomerContext;
import com.automotive.costing.platform.access.entity.*;
import com.automotive.costing.platform.access.repository.*;
import com.automotive.costing.platform.user.service.PermissionMatrixService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Authoritative commercial entitlement + quota layer.
 * License → Module → Feature → Quota → Role Permission → Tenant.
 * Never deletes customer business data on status change.
 */
@Service
@RequiredArgsConstructor
public class CommercialLicenseService {

    private static final Logger log = LoggerFactory.getLogger(CommercialLicenseService.class);

    public static final String LICENSE_NOT_FOUND = "LICENSE_NOT_FOUND";
    public static final String LICENSE_EXPIRED = "LICENSE_EXPIRED";
    public static final String LICENSE_SUSPENDED = "LICENSE_SUSPENDED";
    public static final String LICENSE_PAYMENT_DUE = "LICENSE_PAYMENT_DUE";
    public static final String LICENSE_CANCELLED = "LICENSE_CANCELLED";
    public static final String TOTAL_USER_LIMIT_EXCEEDED = "TOTAL_USER_LIMIT_EXCEEDED";
    public static final String ROLE_USER_LIMIT_EXCEEDED = "ROLE_USER_LIMIT_EXCEEDED";
    public static final String PLANT_LIMIT_EXCEEDED = "PLANT_LIMIT_EXCEEDED";
    public static final String ROLE_NOT_ASSIGNABLE = "ROLE_NOT_ASSIGNABLE";
    public static final String PERMISSION_NOT_ENTITLED = "PERMISSION_NOT_ENTITLED";
    public static final String LICENSE_SIGNATURE_INVALID = "LICENSE_SIGNATURE_INVALID";
    public static final String LICENSE_PACKAGE_INVALID = "LICENSE_PACKAGE_INVALID";

    private final CustomerLicenseRepository licenseRepository;
    private final CustomerRoleLimitRepository roleLimitRepository;
    private final LicenseHistoryRepository historyRepository;
    private final OfflineLicensePackageRepository offlinePackageRepository;
    private final CustomerUserMembershipRepository membershipRepository;
    private final com.automotive.costing.masterdata.repository.PlantMasterRepository plantMasterRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.license.signing-secret:}")
    private String signingSecret;

    // ── Asserts ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CustomerLicense requireLicense(String customerId) {
        return licenseRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "No commercial license for customer", LICENSE_NOT_FOUND));
    }

    /**
     * Operational write path requires an operational license status.
     * Read/admin paths may still access license metadata when suspended/expired.
     */
    @Transactional(readOnly = true)
    public void assertLicenseActive(String customerId) {
        CustomerLicense lic = requireLicense(customerId);
        String status = LicenseStatus.normalize(lic.getStatus());
        LocalDate today = LocalDate.now();

        if (LicenseStatus.SUSPENDED.equals(status)) {
            throw new BusinessRuleViolationException("License is suspended", LICENSE_SUSPENDED);
        }
        if (LicenseStatus.CANCELLED.equals(status)) {
            throw new BusinessRuleViolationException("License is cancelled", LICENSE_CANCELLED);
        }
        if (LicenseStatus.EXPIRED.equals(status)
                || (lic.getValidTo() != null && today.isAfter(lic.getValidTo())
                && (lic.getGraceEndDate() == null || today.isAfter(lic.getGraceEndDate())))) {
            throw new BusinessRuleViolationException("License has expired", LICENSE_EXPIRED);
        }
        if (LicenseStatus.PAYMENT_DUE.equals(status)) {
            // Policy: PAYMENT_DUE still allows ops but surfaces code for UI; block only if past grace
            if (lic.getGraceEndDate() != null && today.isAfter(lic.getGraceEndDate())) {
                throw new BusinessRuleViolationException("Payment overdue past grace", LICENSE_PAYMENT_DUE);
            }
        }
        if (!LicenseStatus.isOperational(status)
                && !LicenseStatus.PAYMENT_DUE.equals(status)
                && !LicenseStatus.GRACE.equals(status)
                && !LicenseStatus.REACTIVATED.equals(status)) {
            throw new BusinessRuleViolationException("License not operational: " + status, LICENSE_SUSPENDED);
        }
    }

    @Transactional
    public void assertTotalUserLimit(String customerId, int additionalUsers) {
        CustomerLicense lic = licenseRepository.findByCustomerIdForUpdate(customerId)
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "No commercial license for customer", LICENSE_NOT_FOUND));
        if (lic.getTotalUserLimit() <= 0) {
            return; // 0 = unlimited
        }
        long current = membershipRepository.countByCustomerIdAndStatus(customerId, MembershipStatus.ACTIVE);
        if (current + additionalUsers > lic.getTotalUserLimit()) {
            throw new BusinessRuleViolationException(
                    "Total user limit exceeded: " + current + "/" + lic.getTotalUserLimit(),
                    TOTAL_USER_LIMIT_EXCEEDED);
        }
    }

    /**
     * Transaction-safe role quota. Locks role_limit row then counts active members with role.
     * additionalAssignments: how many new seats of this role are being added (usually 1).
     */
    @Transactional
    public void assertRoleUserLimit(String customerId, String roleCode, int additionalAssignments) {
        if (roleCode == null || roleCode.isBlank()) {
            return;
        }
        String code = roleCode.trim().toUpperCase(Locale.ROOT);
        Optional<CustomerRoleLimit> locked = roleLimitRepository
                .findByCustomerIdAndRoleCodeForUpdate(customerId, code);
        if (locked.isEmpty()) {
            return; // no limit configured = unlimited for this role
        }
        int max = locked.get().getMaxUsers();
        if (max <= 0) {
            return;
        }
        long current = membershipRepository.countActiveMembersWithRole(
                customerId, MembershipStatus.ACTIVE.name(), code);
        if (current + additionalAssignments > max) {
            throw new BusinessRuleViolationException(
                    "Role user limit exceeded for " + code + ": " + current + "/" + max,
                    ROLE_USER_LIMIT_EXCEEDED);
        }
    }

    /** Enforce limits for a set of roles being assigned to a new or role-changed user. */
    @Transactional
    public void assertRoleAssignmentAllowed(String customerId, Set<String> newRoleCodes, Set<String> previousRoleCodes) {
        Set<String> prev = previousRoleCodes == null ? Set.of() : previousRoleCodes;
        for (String role : newRoleCodes) {
            if (!prev.contains(role)) {
                assertRoleUserLimit(customerId, role, 1);
            }
        }
    }

    /**
     * Transaction-safe plant/site quota. Locks license row then counts plants for customer.
     */
    @Transactional
    public void assertPlantLimit(String customerId, int additionalPlants) {
        if (additionalPlants <= 0) {
            return;
        }
        CustomerLicense lic = licenseRepository.findByCustomerIdForUpdate(customerId)
                .orElse(null);
        if (lic == null) {
            return; // no commercial license configured — unlimited plants
        }
        if (lic.getPlantSiteLimit() <= 0) {
            return; // 0 = unlimited
        }
        long current = plantMasterRepository.countByCustomerId(customerId);
        if (current + additionalPlants > lic.getPlantSiteLimit()) {
            throw new BusinessRuleViolationException(
                    "Plant/site limit exceeded: " + current + "/" + lic.getPlantSiteLimit(),
                    PLANT_LIMIT_EXCEEDED);
        }
    }

    /**
     * Enforce license status only when a commercial license row exists (backward compatible).
     * When no license is configured, operations proceed under existing module/feature/RBAC only.
     */
    @Transactional(readOnly = true)
    public void assertLicenseActiveIfLicensed(String customerId) {
        if (licenseRepository.findByCustomerId(customerId).isEmpty()) {
            return;
        }
        assertLicenseActive(customerId);
    }

    /** Customer Admin may not grant platform-only roles. */
    @Transactional(readOnly = true)
    public void assertRoleAssignableByCustomerAdmin(Set<String> roleCodes) {
        for (String code : roleCodes) {
            if (PermissionMatrixService.CUSTOMER_ADMIN_FORBIDDEN_ROLES.contains(code)
                    || "SYSTEM_ADMINISTRATOR".equals(code)
                    || "PLATFORM_OWNER".equals(code)
                    || "ADMIN".equals(code) && !PermissionMatrixService.CUSTOMER_ADMIN_ASSIGNABLE_ROLES.contains(code)) {
                if (!PermissionMatrixService.CUSTOMER_ADMIN_ASSIGNABLE_ROLES.contains(code)) {
                    throw new BusinessRuleViolationException(
                            "Role not assignable by Customer Admin: " + code, ROLE_NOT_ASSIGNABLE);
                }
            }
            if (PermissionMatrixService.CUSTOMER_ADMIN_FORBIDDEN_ROLES.contains(code)) {
                throw new BusinessRuleViolationException(
                        "Role not assignable by Customer Admin: " + code, ROLE_NOT_ASSIGNABLE);
            }
        }
    }

    // ── Platform Owner mutations ────────────────────────────────────────

    @Transactional
    public CustomerLicense upsertLicense(String customerId, String planCode, String status,
                                         int totalUserLimit, int plantSiteLimit,
                                         LocalDate validFrom, LocalDate validTo,
                                         LocalDate graceEnd, LocalDate paymentDue,
                                         String actorUserId, String reason) {
        Instant now = Instant.now();
        CustomerLicense lic = licenseRepository.findByCustomerId(customerId).orElse(null);
        String before = lic == null ? null : toJson(lic);
        if (lic == null) {
            lic = CustomerLicense.builder()
                    .licenseId(UUID.randomUUID().toString())
                    .customerId(customerId)
                    .createdAt(now)
                    .createdBy(actorUserId)
                    .licenseVersion(1)
                    .overLimit(false)
                    .build();
        } else {
            lic.setLicenseVersion(lic.getLicenseVersion() + 1);
        }
        lic.setPlanCode(planCode);
        lic.setStatus(LicenseStatus.normalize(status));
        lic.setTotalUserLimit(totalUserLimit);
        lic.setPlantSiteLimit(plantSiteLimit);
        lic.setValidFrom(validFrom != null ? validFrom : LocalDate.now());
        lic.setValidTo(validTo);
        lic.setGraceEndDate(graceEnd);
        lic.setPaymentDueDate(paymentDue);
        lic.setUpdatedAt(now);
        lic.setUpdatedBy(actorUserId);

        // Downgrade: mark OVER_LIMIT if current usage exceeds new total limit
        if (lic.getTotalUserLimit() > 0) {
            long current = membershipRepository.countByCustomerIdAndStatus(customerId, MembershipStatus.ACTIVE);
            lic.setOverLimit(current > lic.getTotalUserLimit());
        } else {
            lic.setOverLimit(false);
        }

        CustomerLicense saved = licenseRepository.save(lic);
        recordHistory(customerId, saved.getLicenseId(), "UPSERT_LICENSE", actorUserId, before, toJson(saved), reason);
        return saved;
    }

    @Transactional
    public CustomerRoleLimit setRoleLimit(String customerId, String roleCode, int maxUsers,
                                          String actorUserId, String reason) {
        String code = roleCode.trim().toUpperCase(Locale.ROOT);
        Instant now = Instant.now();
        CustomerRoleLimit rl = roleLimitRepository.findByCustomerIdAndRoleCode(customerId, code).orElse(null);
        String before = rl == null ? null : toJson(rl);
        if (rl == null) {
            rl = CustomerRoleLimit.builder()
                    .roleLimitId(UUID.randomUUID().toString())
                    .customerId(customerId)
                    .roleCode(code)
                    .createdAt(now)
                    .createdBy(actorUserId)
                    .build();
        }
        rl.setMaxUsers(maxUsers);
        rl.setUpdatedAt(now);
        rl.setUpdatedBy(actorUserId);
        CustomerRoleLimit saved = roleLimitRepository.save(rl);
        recordHistory(customerId, null, "SET_ROLE_LIMIT", actorUserId, before, toJson(saved), reason);

        // Mark over-limit on license if role usage exceeds
        licenseRepository.findByCustomerId(customerId).ifPresent(lic -> {
            long current = membershipRepository.countActiveMembersWithRole(
                    customerId, MembershipStatus.ACTIVE.name(), code);
            if (maxUsers > 0 && current > maxUsers) {
                lic.setOverLimit(true);
                lic.setUpdatedAt(Instant.now());
                licenseRepository.save(lic);
            }
        });
        return saved;
    }

    @Transactional
    public CustomerLicense setStatus(String customerId, String status, String actorUserId, String reason) {
        CustomerLicense lic = requireLicense(customerId);
        String before = toJson(lic);
        lic.setStatus(LicenseStatus.normalize(status));
        lic.setUpdatedAt(Instant.now());
        lic.setUpdatedBy(actorUserId);
        CustomerLicense saved = licenseRepository.save(lic);
        recordHistory(customerId, saved.getLicenseId(), "STATUS_" + saved.getStatus(),
                actorUserId, before, toJson(saved), reason);
        return saved;
    }

    // ── Offline signed package ──────────────────────────────────────────

    @Transactional
    public CustomerLicense importOfflinePackage(String customerId, String payloadJson, String signatureHex,
                                                String actorUserId) {
        if (signingSecret == null || signingSecret.isBlank()) {
            throw new BusinessRuleViolationException(
                    "Offline license signing secret not configured (app.license.signing-secret)",
                    LICENSE_PACKAGE_INVALID);
        }
        String expected = hmacHex(payloadJson, signingSecret);
        if (!constantTimeEquals(expected, signatureHex)) {
            throw new BusinessRuleViolationException("License signature invalid", LICENSE_SIGNATURE_INVALID);
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(payloadJson);
        } catch (Exception e) {
            throw new BusinessRuleViolationException("License payload invalid JSON", LICENSE_PACKAGE_INVALID);
        }
        String payloadCustomer = text(root, "customerId");
        if (payloadCustomer == null || !payloadCustomer.equals(customerId)) {
            throw new BusinessRuleViolationException("License customer mismatch", LICENSE_PACKAGE_INVALID);
        }
        String status = text(root, "status");
        int totalUsers = root.path("totalUserLimit").asInt(0);
        int plantLimit = root.path("plantSiteLimit").asInt(0);
        LocalDate validFrom = parseDate(root, "validFrom");
        LocalDate validTo = parseDate(root, "validTo");
        LocalDate graceEnd = parseDate(root, "graceEndDate");
        String planCode = text(root, "planCode");
        int version = root.path("packageVersion").asInt(1);
        LocalDate today = LocalDate.now();
        if (validTo != null && today.isAfter(validTo)
                && (graceEnd == null || today.isAfter(graceEnd))) {
            throw new BusinessRuleViolationException(
                    "Offline license package has expired", LICENSE_EXPIRED);
        }

        OfflineLicensePackage pkg = OfflineLicensePackage.builder()
                .packageId(UUID.randomUUID().toString())
                .customerId(customerId)
                .packageVersion(version)
                .payloadJson(payloadJson)
                .signatureHex(signatureHex)
                .signatureAlg("HmacSHA256")
                .importedAt(Instant.now())
                .importedBy(actorUserId)
                .validFrom(validFrom != null ? validFrom : LocalDate.now())
                .validTo(validTo)
                .status("IMPORTED")
                .build();
        offlinePackageRepository.save(pkg);

        CustomerLicense lic = upsertLicense(customerId, planCode,
                status != null ? status : LicenseStatus.ACTIVE,
                totalUsers, plantLimit, validFrom, validTo, graceEnd, null, actorUserId, "OFFLINE_IMPORT");
        lic.setOfflinePackageId(pkg.getPackageId());
        licenseRepository.save(lic);

        // Role limits from payload
        JsonNode roleLimits = root.path("roleLimits");
        if (roleLimits.isArray()) {
            for (JsonNode n : roleLimits) {
                String role = text(n, "roleCode");
                int max = n.path("maxUsers").asInt(0);
                if (role != null) {
                    setRoleLimit(customerId, role, max, actorUserId, "OFFLINE_IMPORT");
                }
            }
        }
        recordHistory(customerId, lic.getLicenseId(), "OFFLINE_IMPORT", actorUserId, null, payloadJson, null);
        return lic;
    }

    public String signPayload(String payloadJson) {
        if (signingSecret == null || signingSecret.isBlank()) {
            throw new BusinessRuleViolationException(
                    "Signing secret not configured", LICENSE_PACKAGE_INVALID);
        }
        return hmacHex(payloadJson, signingSecret);
    }

    // ── Usage views ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> usageSnapshot(String customerId) {
        Optional<CustomerLicense> opt = licenseRepository.findByCustomerId(customerId);
        long activeUsers = membershipRepository.countByCustomerIdAndStatus(customerId, MembershipStatus.ACTIVE);
        Map<String, Object> snap = new LinkedHashMap<>();
        snap.put("customerId", customerId);
        if (opt.isEmpty()) {
            snap.put("licenseStatus", null);
            snap.put("totalUserLimit", null);
            snap.put("activeUsers", activeUsers);
            snap.put("roleUsage", List.of());
            snap.put("overLimit", false);
            return snap;
        }
        CustomerLicense lic = opt.get();
        snap.put("licenseStatus", lic.getStatus());
        snap.put("planCode", lic.getPlanCode());
        snap.put("totalUserLimit", lic.getTotalUserLimit());
        snap.put("activeUsers", activeUsers);
        snap.put("plantSiteLimit", lic.getPlantSiteLimit());
        long plantCount = plantMasterRepository.countByCustomerId(customerId);
        snap.put("plantSiteUsed", plantCount);
        snap.put("plantSiteOverLimit", lic.getPlantSiteLimit() > 0 && plantCount > lic.getPlantSiteLimit());
        snap.put("validFrom", lic.getValidFrom());
        snap.put("validTo", lic.getValidTo());
        snap.put("graceEndDate", lic.getGraceEndDate());
        snap.put("overLimit", lic.isOverLimit());
        snap.put("licenseVersion", lic.getLicenseVersion());

        List<Map<String, Object>> roleUsage = new ArrayList<>();
        for (CustomerRoleLimit rl : roleLimitRepository.findByCustomerId(customerId)) {
            long used = membershipRepository.countActiveMembersWithRole(
                    customerId, MembershipStatus.ACTIVE.name(), rl.getRoleCode());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("roleCode", rl.getRoleCode());
            row.put("used", used);
            row.put("max", rl.getMaxUsers());
            row.put("overLimit", rl.getMaxUsers() > 0 && used > rl.getMaxUsers());
            roleUsage.add(row);
        }
        snap.put("roleUsage", roleUsage);
        return snap;
    }

    // ── helpers ─────────────────────────────────────────────────────────

    private void recordHistory(String customerId, String licenseId, String action,
                               String actor, String before, String after, String reason) {
        LicenseHistory h = LicenseHistory.builder()
                .historyId(UUID.randomUUID().toString())
                .customerId(customerId)
                .licenseId(licenseId)
                .action(action)
                .actorUserId(actor)
                .beforeJson(before)
                .afterJson(after)
                .reason(reason)
                .correlationId(org.slf4j.MDC.get("correlationId"))
                .createdAt(Instant.now())
                .build();
        historyRepository.save(h);
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return String.valueOf(o);
        }
    }

    private static String hmacHex(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(raw.length * 2);
            for (byte b : raw) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("HMAC failure", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        byte[] x = a.getBytes(StandardCharsets.UTF_8);
        byte[] y = b.getBytes(StandardCharsets.UTF_8);
        if (x.length != y.length) {
            return false;
        }
        int r = 0;
        for (int i = 0; i < x.length; i++) {
            r |= x[i] ^ y[i];
        }
        return r == 0;
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static LocalDate parseDate(JsonNode n, String field) {
        String t = text(n, field);
        if (t == null || t.isBlank()) {
            return null;
        }
        return LocalDate.parse(t);
    }
}
