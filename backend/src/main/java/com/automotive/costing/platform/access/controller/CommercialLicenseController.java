package com.automotive.costing.platform.access.controller;

import com.automotive.costing.platform.access.CustomerContext;
import com.automotive.costing.platform.access.entity.CustomerLicense;
import com.automotive.costing.platform.access.entity.CustomerRoleLimit;
import com.automotive.costing.platform.access.service.CommercialLicenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Platform Owner commercial license management + Customer Admin usage views.
 */
@RestController
@RequestMapping("/api/v1/commercial-license")
@RequiredArgsConstructor
public class CommercialLicenseController {

    private final CommercialLicenseService licenseService;

    // ── Customer Admin: usage snapshot (tenant-scoped) ──────────────────
    @GetMapping("/usage")
    @PreAuthorize("hasAnyRole('CUSTOMER_ADMIN','SYSTEM_ADMINISTRATOR','ADMIN')")
    public Map<String, Object> usage() {
        String customerId = CustomerContext.getCustomerId();
        if (customerId == null || customerId.isBlank()) {
            throw new com.automotive.costing.common.exception.BusinessRuleViolationException(
                    "X-Customer-Id required", "ACCESS-07");
        }
        return licenseService.usageSnapshot(customerId);
    }

    // ── Platform Owner: upsert license ──────────────────────────────────
    @PutMapping("/customers/{customerId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR','ADMIN')")
    @ResponseStatus(HttpStatus.OK)
    public CustomerLicense upsert(
            @PathVariable String customerId,
            @RequestBody UpsertLicenseRequest body,
            Authentication auth) {
        return licenseService.upsertLicense(
                customerId,
                body.planCode(),
                body.status(),
                body.totalUserLimit(),
                body.plantSiteLimit(),
                body.validFrom(),
                body.validTo(),
                body.graceEndDate(),
                body.paymentDueDate(),
                auth != null ? auth.getName() : null,
                body.reason());
    }

    @PutMapping("/customers/{customerId}/role-limits/{roleCode}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR','ADMIN')")
    public CustomerRoleLimit setRoleLimit(
            @PathVariable String customerId,
            @PathVariable String roleCode,
            @RequestBody RoleLimitRequest body,
            Authentication auth) {
        return licenseService.setRoleLimit(
                customerId, roleCode, body.maxUsers(),
                auth != null ? auth.getName() : null, body.reason());
    }

    @PostMapping("/customers/{customerId}/status")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR','ADMIN')")
    public CustomerLicense setStatus(
            @PathVariable String customerId,
            @RequestBody StatusRequest body,
            Authentication auth) {
        return licenseService.setStatus(
                customerId, body.status(),
                auth != null ? auth.getName() : null, body.reason());
    }

    @PostMapping("/customers/{customerId}/offline-import")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR','ADMIN')")
    public CustomerLicense offlineImport(
            @PathVariable String customerId,
            @RequestBody OfflineImportRequest body,
            Authentication auth) {
        return licenseService.importOfflinePackage(
                customerId, body.payloadJson(), body.signatureHex(),
                auth != null ? auth.getName() : null);
    }

    @GetMapping("/customers/{customerId}/usage")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR','ADMIN')")
    public Map<String, Object> platformUsage(@PathVariable String customerId) {
        return licenseService.usageSnapshot(customerId);
    }

    public record UpsertLicenseRequest(
            String planCode, String status, int totalUserLimit, int plantSiteLimit,
            LocalDate validFrom, LocalDate validTo, LocalDate graceEndDate,
            LocalDate paymentDueDate, String reason) {}

    public record RoleLimitRequest(int maxUsers, String reason) {}

    public record StatusRequest(String status, String reason) {}

    public record OfflineImportRequest(String payloadJson, String signatureHex) {}
}
