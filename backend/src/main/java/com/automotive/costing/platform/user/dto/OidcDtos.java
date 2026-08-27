package com.automotive.costing.platform.user.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public final class OidcDtos {
    private OidcDtos() {}

    public record LinkIdentityRequest(
            @NotBlank String userId,
            @NotBlank String issuer,
            @NotBlank String subject,
            String email
    ) {}

    public record OidcLinkResponse(
            String linkId,
            String userId,
            String issuer,
            String subject,
            String email,
            String status,
            Instant linkedAt,
            Instant lastLoginAt
    ) {}
}
