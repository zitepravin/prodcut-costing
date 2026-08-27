package com.automotive.costing.support.controller;

import com.automotive.costing.platform.user.entity.User;
import com.automotive.costing.support.dto.SupportTicketDtos.*;
import com.automotive.costing.support.service.SupportTicketService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/platform/support/tickets")
@RequiredArgsConstructor
@Tag(name = "Support Tickets (Platform)")
public class PlatformSupportTicketController {

    private final SupportTicketService supportTicketService;

    @GetMapping
    @PreAuthorize("@permissionAuth.has('SUPPORT_TICKET_MANAGE') or @permissionAuth.has('SUPPORT_TICKET_READ')")
    public ResponseEntity<Page<PlatformTicketSummary>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String customerId,
            Pageable pageable) {
        return ResponseEntity.ok(supportTicketService.listAll(q, customerId, pageable));
    }

    @GetMapping("/{ticketId}")
    @PreAuthorize("@permissionAuth.has('SUPPORT_TICKET_MANAGE') or @permissionAuth.has('SUPPORT_TICKET_READ')")
    public ResponseEntity<TicketDetail> get(@PathVariable String ticketId) {
        return ResponseEntity.ok(supportTicketService.getPlatform(ticketId));
    }

    @PostMapping("/{ticketId}/messages")
    @PreAuthorize("@permissionAuth.has('SUPPORT_TICKET_MANAGE') or @permissionAuth.has('SUPPORT_TICKET_REPLY')")
    public ResponseEntity<MessageResponse> reply(
            @AuthenticationPrincipal User user,
            @PathVariable String ticketId,
            @Valid @RequestBody MessageRequest request) {
        return ResponseEntity.ok(supportTicketService.replySupport(user, ticketId, request));
    }

    @PostMapping("/{ticketId}/assign")
    @PreAuthorize("@permissionAuth.has('SUPPORT_TICKET_ASSIGN') or @permissionAuth.has('SUPPORT_TICKET_MANAGE')")
    public ResponseEntity<TicketDetail> assign(
            @AuthenticationPrincipal User user,
            @PathVariable String ticketId,
            @Valid @RequestBody AssignRequest request) {
        return ResponseEntity.ok(supportTicketService.assign(user, ticketId, request));
    }

    @PostMapping("/{ticketId}/status")
    @PreAuthorize("@permissionAuth.has('SUPPORT_TICKET_STATUS_UPDATE') or @permissionAuth.has('SUPPORT_TICKET_MANAGE')")
    public ResponseEntity<TicketDetail> status(
            @AuthenticationPrincipal User user,
            @PathVariable String ticketId,
            @Valid @RequestBody StatusRequest request) {
        return ResponseEntity.ok(supportTicketService.changeStatus(user, ticketId, request));
    }

    @PostMapping("/{ticketId}/internal-notes")
    @PreAuthorize("@permissionAuth.has('SUPPORT_TICKET_INTERNAL_NOTE') or @permissionAuth.has('SUPPORT_TICKET_MANAGE')")
    public ResponseEntity<InternalNoteResponse> internalNote(
            @AuthenticationPrincipal User user,
            @PathVariable String ticketId,
            @Valid @RequestBody InternalNoteRequest request) {
        return ResponseEntity.ok(supportTicketService.addInternalNote(user, ticketId, request));
    }
}
