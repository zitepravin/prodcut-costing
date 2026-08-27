package com.automotive.costing.platform.access.controller;

import com.automotive.costing.platform.access.service.CustomerBrandingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * TC-012 — Customer company logo.
 * Authorization: Permission Catalog via {@code @permissionAuth} (not role-name-only).
 * Tenant isolation: CustomerContext only.
 */
@RestController
@RequestMapping("/api/v1/customer/branding")
@RequiredArgsConstructor
@Tag(name = "Customer Branding")
public class CustomerBrandingController {

    private final CustomerBrandingService brandingService;

    @GetMapping("/logo")
    @PreAuthorize("@permissionAuth.has('CUSTOMER_BRANDING_READ')")
    @Operation(summary = "Get current tenant company logo metadata")
    public ResponseEntity<Map<String, Object>> getLogo() {
        return ResponseEntity.ok(brandingService.getLogoMetadata());
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@permissionAuth.has('CUSTOMER_BRANDING_MANAGE')")
    @Operation(summary = "Upload or replace company logo for the active customer/tenant")
    public ResponseEntity<Map<String, Object>> uploadLogo(@RequestPart("file") MultipartFile file)
            throws IOException {
        return ResponseEntity.ok(brandingService.uploadLogo(file));
    }

    @DeleteMapping("/logo")
    @PreAuthorize("@permissionAuth.has('CUSTOMER_BRANDING_MANAGE')")
    @Operation(summary = "Remove company logo for the active customer/tenant")
    public ResponseEntity<Map<String, Object>> clearLogo() {
        return ResponseEntity.ok(brandingService.clearLogo());
    }
}
