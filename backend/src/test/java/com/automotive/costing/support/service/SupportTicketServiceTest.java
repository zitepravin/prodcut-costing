package com.automotive.costing.support.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.notification.service.NotificationService;
import com.automotive.costing.platform.access.CustomerContext;
import com.automotive.costing.platform.user.entity.User;
import com.automotive.costing.support.dto.SupportTicketDtos.CreateRequest;
import com.automotive.costing.support.entity.*;
import com.automotive.costing.support.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupportTicketServiceTest {

    @Mock SupportTicketRepository ticketRepository;
    @Mock SupportTicketMessageRepository messageRepository;
    @Mock SupportTicketInternalNoteRepository noteRepository;
    @Mock SupportTicketSeqRepository seqRepository;
    @Mock AuditService auditService;
    @Mock NotificationService notificationService;

    SupportTicketService service;
    User user;

    @BeforeEach
    void setUp() {
        service = new SupportTicketService(
                ticketRepository, messageRepository, noteRepository, seqRepository,
                auditService, notificationService);
        user = new User();
        user.setUserId("u1");
        CustomerContext.setCustomerId("cust-1");
    }

    @AfterEach
    void tearDown() {
        CustomerContext.clear();
    }

    @Test
    void create_requiresCustomerContext() {
        CustomerContext.clear();
        assertThrows(BusinessRuleViolationException.class, () ->
                service.create(user, new CreateRequest("subj", SupportTicketCategory.BOM,
                        SupportTicketPriority.HIGH, "desc")));
    }

    @Test
    void create_persistsTicketAndInitialMessage() {
        when(seqRepository.nextTicketNumber()).thenReturn("ST-000001");
        when(ticketRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(messageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(messageRepository.findByTicketIdOrderByCreatedAtAsc(any())).thenReturn(java.util.List.of());

        var detail = service.create(user, new CreateRequest(
                "BOM upload error", SupportTicketCategory.BOM,
                SupportTicketPriority.MEDIUM, "Cannot upload BOM file"));

        assertEquals("ST-000001", detail.ticketNumber());
        assertEquals(SupportTicketStatus.OPEN, detail.status());
        verify(auditService).recordCreate(eq("SupportTicket"), anyString(), any());
        verify(notificationService).publish(anyString(), any(), anyString(), anyString(), anyString(), anyString(), any());
    }
}
