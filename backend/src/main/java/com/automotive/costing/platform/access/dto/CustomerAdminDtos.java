package com.automotive.costing.platform.access.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public final class CustomerAdminDtos {

    private CustomerAdminDtos() {}

    public record CreateCustomerUserRequest(
            @NotBlank @Size(max = 200) String employeeName,
            @NotBlank @Email @Size(max = 150) String email,
            @Size(max = 50) String employeeId,
            String departmentId,
            @Size(max = 100) String designation,
            @Size(max = 30) String mobileNumber,
            @NotEmpty Set<String> roleCodes
    ) {}

    public record UpdateCustomerUserRequest(
            @Size(max = 200) String employeeName,
            @Size(max = 50) String employeeId,
            String departmentId,
            @Size(max = 100) String designation,
            @Size(max = 30) String mobileNumber,
            Set<String> roleCodes
    ) {}

    public record CustomerUserResponse(
            String userId,
            String employeeName,
            String email,
            String username,
            String employeeId,
            String departmentId,
            String departmentName,
            String designation,
            String mobileNumber,
            String status,
            Set<String> roleCodes,
            Instant lastLoginAt,
            Instant createdAt
    ) {}

    public record CreateDepartmentRequest(
            @NotBlank @Size(max = 50) String departmentCode,
            @NotBlank @Size(max = 150) String departmentName,
            @Size(max = 500) String description
    ) {}

    public record UpdateDepartmentRequest(
            @Size(max = 150) String departmentName,
            @Size(max = 500) String description,
            String status
    ) {}

    public record DepartmentResponse(
            String departmentId,
            String customerId,
            String departmentCode,
            String departmentName,
            String description,
            String status
    ) {}

    public record AcceptInvitationRequest(
            @NotBlank String token,
            @NotBlank @Size(min = 8, max = 128) String password
    ) {}

    public record InvitationAcceptResponse(
            String userId,
            String email,
            String status
    ) {}

    public record CreateUserResult(
            CustomerUserResponse user,
            boolean invitationCreated,
            /** Present only in non-production / when email not configured — never the raw secret in logs. */
            String invitationDeliveryNote
    ) {}
}
