package com.automotive.costing.platform.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

public class AuthDtos {

    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {
        @NotBlank(message = "username is required")
        public String username;

        @NotBlank(message = "password is required")
        public String password;

        /** SEC-07: required (and verified) only if the resolved user has mfaEnabled=true. */
        public String mfaCode;
    }

    public record TokenResponse(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresInMs
    ) {
    }

    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefreshRequest {
        @NotBlank(message = "refreshToken is required")
        public String refreshToken;
    }
}
