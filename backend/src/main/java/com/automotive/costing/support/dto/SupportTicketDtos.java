package com.automotive.costing.support.dto;

import com.automotive.costing.support.entity.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class SupportTicketDtos {
    private SupportTicketDtos() {}

    public record CreateRequest(
            @NotBlank @Size(max = 200) String subject,
            @NotNull SupportTicketCategory category,
            @NotNull SupportTicketPriority priority,
            @NotBlank @Size(max = 4000) String description
    ) {}

    public record MessageRequest(@NotBlank @Size(max = 4000) String message) {}

    public record StatusRequest(@NotNull SupportTicketStatus status) {}

    public record AssignRequest(@NotBlank String assigneeUserId) {}

    public record InternalNoteRequest(@NotBlank @Size(max = 4000) String note) {}

    public record MessageResponse(
            String messageId, String authorUserId, SupportAuthorType authorType,
            String message, Instant createdAt
    ) {}

    public record InternalNoteResponse(
            String noteId, String authorUserId, String note, Instant createdAt
    ) {}

    public record TicketSummary(
            String ticketId, String ticketNumber, String subject,
            SupportTicketCategory category, SupportTicketPriority priority,
            SupportTicketStatus status, String assignedToUserId,
            Instant createdAt, Instant updatedAt
    ) {}

    public record TicketDetail(
            String ticketId, String ticketNumber, String customerId,
            String subject, String description,
            SupportTicketCategory category, SupportTicketPriority priority,
            SupportTicketStatus status, String createdByUserId, String assignedToUserId,
            Instant createdAt, Instant updatedAt, Instant resolvedAt, Instant closedAt,
            List<MessageResponse> messages,
            List<InternalNoteResponse> internalNotes
    ) {}

    public record PlatformTicketSummary(
            String ticketId, String ticketNumber, String customerId,
            String subject, SupportTicketCategory category, SupportTicketPriority priority,
            SupportTicketStatus status, String assignedToUserId,
            Instant createdAt, Instant updatedAt
    ) {}
}
