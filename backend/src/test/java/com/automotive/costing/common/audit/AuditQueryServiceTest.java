package com.automotive.costing.common.audit;

import com.automotive.costing.common.audit.dto.AuditDtos.AuditLogResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditQueryServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditQueryService auditQueryService;

    private AuditLogEntry sampleEntry;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        sampleEntry = AuditLogEntry.builder()
                .auditId("a1")
                .entityType("ErpInboundBatch")
                .entityId("b1")
                .action("CREATE")
                .oldValue(null)
                .newValue("{\"batchId\":\"b1\"}")
                .userId("admin")
                .timestamp(Instant.parse("2026-08-13T10:00:00Z"))
                .ipAddress("127.0.0.1")
                .build();
        pageable = PageRequest.of(0, 20);
    }

    @Test
    @DisplayName("findByEntity maps repository page to DTOs")
    void findByEntity_mapsToDto() {
        when(auditLogRepository.findByEntityTypeAndEntityId(eq("ErpInboundBatch"), eq("b1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleEntry), pageable, 1));

        Page<AuditLogResponse> result = auditQueryService.findByEntity("ErpInboundBatch", "b1", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        AuditLogResponse r = result.getContent().get(0);
        assertThat(r.auditId()).isEqualTo("a1");
        assertThat(r.entityType()).isEqualTo("ErpInboundBatch");
        assertThat(r.entityId()).isEqualTo("b1");
        assertThat(r.action()).isEqualTo("CREATE");
        assertThat(r.userId()).isEqualTo("admin");
        verify(auditLogRepository).findByEntityTypeAndEntityId("ErpInboundBatch", "b1", pageable);
    }

    @Test
    @DisplayName("findByUser applies default time bounds when null")
    void findByUser_defaultBounds() {
        when(auditLogRepository.findByUserIdAndTimestampBetween(eq("admin"), any(Instant.class), any(Instant.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleEntry), pageable, 1));

        Page<AuditLogResponse> result = auditQueryService.findByUser("admin", null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(auditLogRepository).findByUserIdAndTimestampBetween(eq("admin"), eq(Instant.EPOCH), any(Instant.class), eq(pageable));
    }

    @Test
    @DisplayName("findByEntityType maps results")
    void findByEntityType_maps() {
        Instant from = Instant.parse("2026-08-01T00:00:00Z");
        Instant to = Instant.parse("2026-08-14T00:00:00Z");
        when(auditLogRepository.findByEntityTypeAndTimestampBetween(eq("MaterialMaster"), eq(from), eq(to), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<AuditLogResponse> result = auditQueryService.findByEntityType("MaterialMaster", from, to, pageable);

        assertThat(result.getTotalElements()).isZero();
        verify(auditLogRepository).findByEntityTypeAndTimestampBetween("MaterialMaster", from, to, pageable);
    }
}
