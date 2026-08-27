package com.automotive.costing.support.repository;

import com.automotive.costing.support.entity.SupportTicket;
import com.automotive.costing.support.entity.SupportTicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, String> {
    Page<SupportTicket> findByCustomerIdOrderByUpdatedAtDesc(String customerId, Pageable pageable);
    Page<SupportTicket> findByCustomerIdAndStatusOrderByUpdatedAtDesc(
            String customerId, SupportTicketStatus status, Pageable pageable);
    Optional<SupportTicket> findByTicketIdAndCustomerId(String ticketId, String customerId);
    Page<SupportTicket> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    long countByCustomerId(String customerId);
    long countByCustomerIdAndStatus(String customerId, SupportTicketStatus status);
}
