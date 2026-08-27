package com.automotive.costing.platform.access.repository;

import com.automotive.costing.platform.access.entity.SupportAccessSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SupportAccessSessionRepository extends JpaRepository<SupportAccessSession, String> {
    List<SupportAccessSession> findByCustomerIdOrderByRequestedAtDesc(String customerId);
    List<SupportAccessSession> findByRequestedByUserIdOrderByRequestedAtDesc(String userId);
    Optional<SupportAccessSession> findFirstByCustomerIdAndRequestedByUserIdAndStatusAndExpiresAtAfter(
            String customerId, String userId, String status, Instant now);
}
