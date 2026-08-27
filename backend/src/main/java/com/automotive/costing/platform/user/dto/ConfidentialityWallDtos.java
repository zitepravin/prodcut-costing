package com.automotive.costing.platform.user.dto;

import jakarta.validation.constraints.NotBlank;

public class ConfidentialityWallDtos {

    public record CreateRequest(
            @NotBlank(message = "rfqEventReference is required") String rfqEventReference,
            String allowedRoleCode,
            String allowedUserId
    ) {
    }

    public record Response(
            String wallId,
            String rfqEventReference,
            String allowedRoleCode,
            String allowedUserId
    ) {
    }
}
