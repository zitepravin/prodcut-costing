package com.automotive.costing.platform.access.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public final class AccessDtos {
    private AccessDtos() {}

    public record CreateCustomerRequest(
            @NotBlank @Size(max = 50) String customerCode,
            @NotBlank @Size(max = 200) String customerName
    ) {}

    public record UpdateCustomerRequest(
            @NotBlank @Size(max = 200) String customerName
    ) {}

    public record SetCustomerStatusRequest(
            @NotBlank String status
    ) {}

    public record CustomerResponse(
            String customerId,
            String customerCode,
            String customerName,
            String status
    ) {}

    public record ModuleResponse(
            String moduleId,
            String moduleCode,
            String moduleName,
            String description,
            String status
    ) {}

    public record SetEntitlementRequest(
            @NotBlank String customerId,
            @NotBlank String moduleCode,
            boolean enabled,
            LocalDate effectiveFrom,
            LocalDate effectiveTo
    ) {}

    public record EntitlementResponse(
            String entitlementId,
            String customerId,
            String moduleId,
            String moduleCode,
            boolean enabled,
            LocalDate effectiveFrom,
            LocalDate effectiveTo
    ) {}

    public record AddMembershipRequest(
            @NotBlank String customerId,
            @NotBlank String userId
    ) {}

    public record MembershipResponse(
            String membershipId,
            String customerId,
            String userId,
            String status
    ) {}

    public record EffectiveAccessResponse(
            String userId,
            String username,
            List<CustomerAccess> customers
    ) {}

    public record CustomerAccess(
            String customerId,
            String customerCode,
            String customerName,
            List<String> enabledModuleCodes,
            List<String> roleCodes
    ) {}
}
