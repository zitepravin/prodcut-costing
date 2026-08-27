package com.automotive.costing.support.controller;

import com.automotive.costing.common.file.StoredFileEntity;
import com.automotive.costing.common.file.StoredFileService;
import com.automotive.costing.platform.user.entity.User;
import com.automotive.costing.support.dto.SupportTicketDtos.*;
import com.automotive.costing.support.entity.SupportTicketCategory;
import com.automotive.costing.support.entity.SupportTicketPriority;
import com.automotive.costing.support.entity.SupportTicketStatus;
import com.automotive.costing.support.service.SupportTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/support/tickets")
@RequiredArgsConstructor
@Tag(name = "Support Tickets (Customer)")
public class SupportTicketController {

    private final SupportTicketService supportTicketService;
    private final StoredFileService storedFileService;

    @GetMapping
    @PreAuthorize("@permissionAuth.has('SUPPORT_TICKET_READ')")
    public ResponseEntity<Page<TicketSummary>> list(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) SupportTicketStatus status,
            @RequestParam(required = false) SupportTicketPriority priority,
            @RequestParam(required = false) SupportTicketCategory category,
            @RequestParam(required = false) String q,
            Pageable pageable) {
        return ResponseEntity.ok(supportTicketService.listMine(user, status, priority, category, q, pageable));
    }

    @PostMapping
    @PreAuthorize("@permissionAuth.has('SUPPORT_TICKET_CREATE')")
    public ResponseEntity<TicketDetail> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(supportTicketService.create(user, request));
    }

    @GetMapping("/{ticketId}")
    @PreAuthorize("@permissionAuth.has('SUPPORT_TICKET_READ')")
    public ResponseEntity<TicketDetail> get(
            @AuthenticationPrincipal User user,
            @PathVariable String ticketId) {
        return ResponseEntity.ok(supportTicketService.getMine(user, ticketId));
    }

    @PostMapping("/{ticketId}/messages")
    @PreAuthorize("@permissionAuth.has('SUPPORT_TICKET_REPLY')")
    public ResponseEntity<MessageResponse> reply(
            @AuthenticationPrincipal User user,
            @PathVariable String ticketId,
            @Valid @RequestBody MessageRequest request) {
        return ResponseEntity.ok(supportTicketService.replyCustomer(user, ticketId, request));
    }

    @PostMapping("/{ticketId}/close")
    @PreAuthorize("@permissionAuth.has('SUPPORT_TICKET_CLOSE')")
    public ResponseEntity<TicketDetail> close(
            @AuthenticationPrincipal User user,
            @PathVariable String ticketId) {
        return ResponseEntity.ok(supportTicketService.closeCustomer(user, ticketId));
    }

    @PostMapping(value = "/{ticketId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@permissionAuth.has('SUPPORT_TICKET_REPLY') or @permissionAuth.has('SUPPORT_TICKET_CREATE')")
    @Operation(summary = "Attach file to ticket (StoredFileService, tenant-scoped)")
    public ResponseEntity<Map<String, Object>> attach(
            @AuthenticationPrincipal User user,
            @PathVariable String ticketId,
            @RequestPart("file") MultipartFile file) throws IOException {
        supportTicketService.assertCustomerCanAccess(ticketId);
        StoredFileEntity meta = storedFileService.store("SUPPORT_TICKET", ticketId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "fileId", meta.getFileId(),
                "fileName", meta.getFileName(),
                "contentType", meta.getContentType() != null ? meta.getContentType() : "",
                "sizeBytes", meta.getSizeBytes()
        ));
    }

    @GetMapping("/{ticketId}/attachments")
    @PreAuthorize("@permissionAuth.has('SUPPORT_TICKET_READ')")
    public ResponseEntity<List<Map<String, Object>>> listAttachments(
            @PathVariable String ticketId) {
        supportTicketService.assertCustomerCanAccess(ticketId);
        List<Map<String, Object>> list = storedFileService.listForEntity("SUPPORT_TICKET", ticketId).stream()
                .map(f -> Map.<String, Object>of(
                        "fileId", f.getFileId(),
                        "fileName", f.getFileName(),
                        "contentType", f.getContentType() != null ? f.getContentType() : "",
                        "sizeBytes", f.getSizeBytes()))
                .toList();
        return ResponseEntity.ok(list);
    }
}
