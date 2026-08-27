package com.automotive.costing.support.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.notification.service.NotificationService;
import com.automotive.costing.platform.access.CustomerContext;
import com.automotive.costing.platform.user.entity.User;
import com.automotive.costing.support.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupportTicketTenantIsolationTest {

    @Mock SupportTicketRepository ticketRepository;
    @Mock SupportTicketMessageRepository messageRepository;
    @Mock SupportTicketInternalNoteRepository noteRepository;
    @Mock SupportTicketSeqRepository seqRepository;
    @Mock AuditService auditService;
    @Mock NotificationService notificationService;

    SupportTicketService service;
    User userA;

    @BeforeEach
    void setUp() {
        service = new SupportTicketService(
                ticketRepository, messageRepository, noteRepository, seqRepository,
                auditService, notificationService);
        userA = new User();
        userA.setUserId("user-a");
        CustomerContext.setCustomerId("customer-a");
    }

    @AfterEach
    void tearDown() {
        CustomerContext.clear();
    }

    @Test
    void customerA_cannotGetCustomerBTicket() {
        when(ticketRepository.findByTicketIdAndCustomerId("ticket-b", "customer-a"))
                .thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getMine(userA, "ticket-b"));
    }
}
