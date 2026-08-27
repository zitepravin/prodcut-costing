package com.automotive.costing.platform.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.Instant;
import java.util.Set;

public class AdminDtos {

    public record CreateUserRequest(
            @NotBlank(message = "username is required") String username,
            @NotBlank(message = "email is required") @Email String email,
            @NotBlank(message = "password is required") String password,
            @NotEmpty(message = "at least one role is required") Set<String> roleCodes
    ) {
    }

    public record UserResponse(
            String userId,
            String username,
            String email,
            String status,
            boolean mfaEnabled,
            Set<String> roleCodes,
            Instant lastLoginAt
    ) {
    }

    public record UpdateRolesRequest(
            @NotEmpty(message = "at least one role is required") Set<String> roleCodes
    ) {
    }

    public record CreateRoleRequest(
            @NotBlank(message = "roleCode is required") String roleCode,
            String description
    ) {
    }

    public record RoleResponse(
            String roleId,
            String roleCode,
            String description,
            boolean systemRole
    ) {
    }

    public record GrantScopeRequest(
            @NotBlank(message = "scopeType is required") String scopeType, // PLANT / PROGRAM / COMMODITY
            @NotBlank(message = "scopeValue is required") String scopeValue
    ) {
    }

    public record ScopeResponse(
            String scopeId,
            String userId,
            String scopeType,
            String scopeValue
    ) {
    }
}
