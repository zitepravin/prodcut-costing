package com.automotive.costing.platform.user.controller;

import com.automotive.costing.platform.user.dto.OidcDtos.LinkIdentityRequest;
import com.automotive.costing.platform.user.dto.OidcDtos.OidcLinkResponse;
import com.automotive.costing.platform.user.service.OidcIdentityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/oidc")
@RequiredArgsConstructor
@Tag(name = "OIDC Identity (SEC-02)")
public class OidcAdminController {

    private final OidcIdentityService oidcIdentityService;

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Whether live OIDC login is enabled in this deployment")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of("enabled", oidcIdentityService.isOidcEnabled()));
    }

    @PostMapping("/links")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Link an IdP issuer+subject to an internal user")
    public ResponseEntity<OidcLinkResponse> link(@Valid @RequestBody LinkIdentityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(oidcIdentityService.link(request));
    }

    @PostMapping("/links/{linkId}/revoke")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Revoke an OIDC identity link")
    public ResponseEntity<OidcLinkResponse> revoke(@PathVariable String linkId) {
        return ResponseEntity.ok(oidcIdentityService.revoke(linkId));
    }

    @GetMapping("/users/{userId}/links")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "List active OIDC links for a user")
    public ResponseEntity<List<OidcLinkResponse>> list(@PathVariable String userId) {
        return ResponseEntity.ok(oidcIdentityService.listForUser(userId));
    }

    @GetMapping("/resolve")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMINISTRATOR', 'ADMIN')")
    @Operation(summary = "Resolve issuer+subject to linked user")
    public ResponseEntity<OidcLinkResponse> resolve(
            @RequestParam String issuer,
            @RequestParam String subject) {
        return ResponseEntity.ok(oidcIdentityService.resolve(issuer, subject));
    }
}
