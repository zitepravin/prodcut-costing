package com.automotive.costing.purchase.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;

public final class SapPoLinkDtos {
    private SapPoLinkDtos() {}

    public record SapPoLinkRequest(
            @NotBlank String externalPoNumber,
            String externalPoSystem,
            String sourceEventId,
            Instant postedAt,
            String notes
    ) {}

    public record HumanPoLinkRequest(
            @NotBlank String externalPoNumber,
            @NotBlank String reason,
            String notes
    ) {}

    public record SapPoLinkResponse(
            String purchaseRequestId,
            String status,
            String externalPoReference,
            String externalPoSystem,
            String poLinkStatus,
            Instant poLinkedAt,
            String poLinkedSource,
            String poLinkEventId,
            List<String> policyNotes
    ) {}
}
