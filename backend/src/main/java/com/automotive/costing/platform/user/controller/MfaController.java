package com.automotive.costing.platform.user.controller;

import com.automotive.costing.platform.user.entity.User;
import com.automotive.costing.platform.user.service.MfaService;
import com.automotive.costing.platform.user.service.MfaService.EnrollResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** SEC-07: self-service MFA enrollment for the currently authenticated user. */
@RestController
@RequestMapping("/api/v1/auth/mfa")
@RequiredArgsConstructor
@Tag(name = "Multi-Factor Authentication")
public class MfaController {

    private final MfaService mfaService;

    @PostMapping("/enroll")
    @Operation(summary = "Generate a new MFA secret for the current user - not yet enforced until confirmed")
    public ResponseEntity<EnrollResponse> enroll(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(mfaService.enroll(user.getUserId()));
    }

    @PostMapping("/confirm")
    @Operation(summary = "Confirm enrollment with one valid TOTP code - flips mfaEnabled on")
    public ResponseEntity<Void> confirm(@AuthenticationPrincipal User user, @RequestParam String code) {
        mfaService.confirm(user.getUserId(), code);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/disable")
    @Operation(summary = "Disable MFA for the current user")
    public ResponseEntity<Void> disable(@AuthenticationPrincipal User user) {
        mfaService.disable(user.getUserId());
        return ResponseEntity.noContent().build();
    }
}
