package com.automotive.costing.common.audit;

import com.automotive.costing.common.audit.dto.AuditDtos.AuditLogResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditQueryServiceActionTest {

    @Mock private AuditLogRepository auditLogRepository;
    @InjectMocks private AuditQueryService auditQueryService;

    private final Pageable pageable = PageRequest.of(0, 20);

    private AuditLogEntry sample(String action) {
        return AuditLogEntry.newEntry()
                .entityType("SupplierWarrantyClaim")
                .entityId("c1")
                .action(action)
                .userId("u1")
                .build();
    }

    @Test
    void findByAction_delegatesToRepository() {
        when(auditLogRepository.findByActionAndTimestampBetween(
                eq("TRANSITION"), any(Instant.class), any(Instant.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(sample("TRANSITION")), pageable, 1));

        var page = auditQueryService.findByAction("TRANSITION", null, null, pageable);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).action()).isEqualTo("TRANSITION");
        verify(auditLogRepository).findByActionAndTimestampBetween(
                eq("TRANSITION"), any(Instant.class), any(Instant.class), eq(pageable));
    }

    @Test
    void findByEntityTypeAndAction_delegatesToRepository() {
        when(auditLogRepository.findByEntityTypeAndActionAndTimestampBetween(
                eq("SupplierRisk"), eq("TRANSITION"), any(Instant.class), any(Instant.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(sample("TRANSITION")), pageable, 1));

        var page = auditQueryService.findByEntityTypeAndAction(
                "SupplierRisk", "TRANSITION", null, null, pageable);
        assertThat(page.getContent()).hasSize(1);
        verify(auditLogRepository).findByEntityTypeAndActionAndTimestampBetween(
                eq("SupplierRisk"), eq("TRANSITION"), any(Instant.class), any(Instant.class), eq(pageable));
    }
}
