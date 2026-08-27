package com.automotive.costing.common.audit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditQueryServiceBoundsTest {

    @Mock private AuditLogRepository auditLogRepository;
    @InjectMocks private AuditQueryService auditQueryService;
    private final Pageable pageable = PageRequest.of(0, 10);

    private AuditLogEntry sample() {
        return AuditLogEntry.newEntry()
                .entityType("Material")
                .entityId("m1")
                .action("CREATE")
                .userId("u1")
                .build();
    }

    @Test
    void findByEntityType_withExplicitBounds() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-12-31T00:00:00Z");
        when(auditLogRepository.findByEntityTypeAndTimestampBetween(
                eq("Material"), eq(from), eq(to), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(sample()), pageable, 1));
        auditQueryService.findByEntityType("Material", from, to, pageable);
        verify(auditLogRepository).findByEntityTypeAndTimestampBetween("Material", from, to, pageable);
    }

    @Test
    void findByEntityTypeAndAction_withExplicitBounds() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-06-01T00:00:00Z");
        when(auditLogRepository.findByEntityTypeAndActionAndTimestampBetween(
                eq("SupplierRisk"), eq("TRANSITION"), eq(from), eq(to), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(sample()), pageable, 1));
        auditQueryService.findByEntityTypeAndAction("SupplierRisk", "TRANSITION", from, to, pageable);
        verify(auditLogRepository).findByEntityTypeAndActionAndTimestampBetween(
                "SupplierRisk", "TRANSITION", from, to, pageable);
    }

    @Test
    void findByAction_withExplicitBounds() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-06-01T00:00:00Z");
        when(auditLogRepository.findByActionAndTimestampBetween(
                eq("VERIFY"), eq(from), eq(to), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(sample()), pageable, 1));
        auditQueryService.findByAction("VERIFY", from, to, pageable);
        verify(auditLogRepository).findByActionAndTimestampBetween("VERIFY", from, to, pageable);
    }

    @Test
    void findByUser_withExplicitBounds() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-06-01T00:00:00Z");
        when(auditLogRepository.findByUserIdAndTimestampBetween(
                eq("u1"), eq(from), eq(to), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(sample()), pageable, 1));
        auditQueryService.findByUser("u1", from, to, pageable);
        verify(auditLogRepository).findByUserIdAndTimestampBetween("u1", from, to, pageable);
    }
}
