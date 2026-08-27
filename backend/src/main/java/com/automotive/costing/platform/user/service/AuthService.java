package com.automotive.costing.platform.user.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.config.JwtProperties;
import com.automotive.costing.platform.user.dto.AuthDtos.LoginRequest;
import com.automotive.costing.platform.user.dto.AuthDtos.RefreshRequest;
import com.automotive.costing.platform.user.dto.AuthDtos.TokenResponse;
import com.automotive.costing.platform.user.entity.User;
import com.automotive.costing.platform.user.repository.UserRepository;
import com.automotive.costing.security.JwtTokenProvider;
import com.automotive.costing.security.LoginRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Primary authentication, MFA, refresh rotation, and logout revocation (tokenVersion).
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final TotpService totpService;
    private final AuditService auditService;
    private final LoginAttemptTracker loginAttemptTracker;
    private final JwtProperties jwtProperties;
    private final LoginRateLimiter loginRateLimiter;

    @Transactional
    public TokenResponse login(LoginRequest request) {
        loginRateLimiter.check("login:" + request.username);
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username, request.password));
        } catch (BadCredentialsException e) {
            loginAttemptTracker.recordFailedAttempt(request.username);
            auditService.recordAuthEvent("LOGIN_FAILURE", resolveUserIdOrUnknown(request.username));
            throw e;
        }
        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = (User) authentication.getPrincipal();

        if (user.isMfaEnabled()) {
            if (user.getMfaSecret() == null || !totpService.verifyCode(user.getMfaSecret(), request.mfaCode)) {
                auditService.recordAuthEvent("LOGIN_MFA_FAILURE", user.getUserId());
                throw new BusinessRuleViolationException(
                        "A valid MFA code is required for this account.", "SEC-07-mfa-required");
            }
        }

        user.setLastLoginAt(Instant.now());
        user.setFailedLoginAttempts(0);
        userRepository.save(user);
        auditService.recordAuthEvent("LOGIN_SUCCESS", user.getUserId());
        loginRateLimiter.reset("login:" + request.username);

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        return new TokenResponse(accessToken, refreshToken, "Bearer", jwtProperties.getAccessTokenExpirationMs());
    }

    /**
     * Validates refresh token, issues new access + new refresh (rotation).
     * Previous refresh remains valid until its natural expiry unless tokenVersion is bumped.
     */
    @Transactional(readOnly = true)
    public TokenResponse refresh(RefreshRequest request) {
        loginRateLimiter.check("refresh:" + Integer.toHexString(request.refreshToken.hashCode()));
        String username;
        try {
            username = jwtTokenProvider.extractUsername(request.refreshToken);
        } catch (Exception e) {
            throw new BadCredentialsException("Refresh token is invalid, expired, or has been revoked.");
        }
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (!jwtTokenProvider.isRefreshToken(request.refreshToken)
                || !jwtTokenProvider.isTokenValid(request.refreshToken, userDetails)) {
            throw new BadCredentialsException("Refresh token is invalid, expired, or has been revoked.");
        }

        String newAccessToken = jwtTokenProvider.generateAccessToken(userDetails);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(userDetails);
        return new TokenResponse(newAccessToken, newRefreshToken, "Bearer",
                jwtProperties.getAccessTokenExpirationMs());
    }

    /**
     * SEC-05/SEC-08: bump tokenVersion so outstanding access and refresh JWTs fail validation.
     */
    @Transactional
    public void logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            User managed = userRepository.findById(user.getUserId()).orElse(user);
            managed.setTokenVersion(managed.getTokenVersion() + 1);
            userRepository.save(managed);
            auditService.recordAuthEvent("LOGOUT", managed.getUserId());
        }
        SecurityContextHolder.clearContext();
    }

    private String resolveUserIdOrUnknown(String username) {
        return userRepository.findByUsername(username).map(User::getUserId).orElse("UNKNOWN");
    }
}
