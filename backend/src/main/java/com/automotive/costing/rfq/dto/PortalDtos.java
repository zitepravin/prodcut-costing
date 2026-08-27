package com.automotive.costing.rfq.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public final class PortalDtos {

    public record GrantRfqAccessRequest(
            @NotBlank String supplierId,
            @NotBlank String rfqId
    ) {}

    public record PortalRfqAccessResponse(
            String accessId,
            String supplierId,
            String rfqId,
            String status,
            java.time.Instant grantedAt
    ) {}

    private PortalDtos() {}

    public record CreateInviteRequest(
            @NotBlank String supplierId,
            @NotBlank @Email String email
    ) {}

    public record InviteResponse(
            String inviteId,
            String supplierId,
            String email,
            Instant expiresAt,
            /** Raw token returned only at creation — store/send to supplier; not retrievable later. */
            String inviteToken
    ) {}

    public record AcceptInviteRequest(
            @NotBlank String inviteToken,
            @NotBlank @Size(min = 10, max = 100) String password
    ) {}

    public record PortalLoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}

    public record PortalTokenResponse(
            String accessToken,
            String tokenType,
            long expiresInMs,
            String supplierId,
            String email
    ) {}

    public record PortalQuoteSubmitRequest(
            @NotBlank String rfqId,
            @NotBlank String rfqLineId,
            @NotNull BigDecimal unitPrice,
            @Size(max = 3) String currencyCode,
            BigDecimal toolingLumpSum,
            Integer leadTimeDays,
            LocalDate validFrom,
            LocalDate validTo,
            String notes
    ) {}

    public record CreateClarificationRequest(
            @NotBlank String rfqId,
            @NotBlank @Size(max = 2000) String questionText
    ) {}

    public record ClarificationResponse(
            String clarificationId,
            String rfqId,
            String supplierId,
            String questionText,
            String status,
            String buyerResponse,
            java.time.Instant respondedAt,
            java.time.Instant createdAt
    ) {}

    public record RespondClarificationRequest(
            @NotBlank @Size(max = 2000) String buyerResponse
    ) {}

    public record CreateAttachmentRequest(
            @NotBlank String rfqId,
            @NotBlank @Size(max = 255) String fileName,
            @Size(max = 100) String contentType,
            @NotBlank @Size(max = 1000) String storageUri,
            @Size(max = 500) String notes
    ) {}

    public record AttachmentResponse(
            String attachmentId,
            String rfqId,
            String fileName,
            String contentType,
            String storageUri,
            String uploadedBy,
            java.time.Instant uploadedAt,
            String notes
    ) {}

}
