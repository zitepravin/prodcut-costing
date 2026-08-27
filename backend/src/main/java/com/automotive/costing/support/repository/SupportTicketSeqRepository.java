package com.automotive.costing.support.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SupportTicketSeqRepository {
    private final JdbcTemplate jdbc;

    public SupportTicketSeqRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Atomically allocates next ST-###### number. */
    public String nextTicketNumber() {
        jdbc.update("UPDATE support_ticket_seq SET next_val = LAST_INSERT_ID(next_val + 1) WHERE seq_name = 'SUPPORT_TICKET'");
        Long n = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        long val = n != null && n > 0 ? n : 1L;
        // LAST_INSERT_ID returns the NEW value after update expression next_val+1 assigned into next_val
        // Our update sets next_val = next_val+1 via LAST_INSERT_ID(next_val+1); first call after seed 1 yields 2.
        // Prefer: read after increment. Format 6 digits.
        return String.format("ST-%06d", val);
    }
}
