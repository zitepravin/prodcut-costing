package com.automotive.costing.support.repository;

import com.automotive.costing.support.entity.SupportTicketInternalNote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SupportTicketInternalNoteRepository extends JpaRepository<SupportTicketInternalNote, String> {
    List<SupportTicketInternalNote> findByTicketIdOrderByCreatedAtAsc(String ticketId);
}
