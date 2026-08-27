package com.automotive.costing.erp.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.erp.dto.ErpDtos.IngestRequest;
import com.automotive.costing.erp.dto.ErpDtos.PartRow;
import com.automotive.costing.erp.entity.ErpBatchType;
import com.automotive.costing.erp.repository.ErpBomStagingRepository;
import com.automotive.costing.erp.repository.ErpInboundBatchRepository;
import com.automotive.costing.erp.repository.ErpMaterialPriceStagingRepository;
import com.automotive.costing.erp.repository.ErpPartStagingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ErpStagingServiceTest {

    @Mock ErpInboundBatchRepository batchRepository;
    @Mock private TenantAccessGuard tenantAccessGuard;
    @Mock ErpPartStagingRepository partRepository;
    @Mock ErpBomStagingRepository bomRepository;
    @Mock ErpMaterialPriceStagingRepository priceRepository;
    @Mock AuditService auditService;

    ErpStagingService service;

    @BeforeEach
    void setUp() {
        service = new ErpStagingService(batchRepository, tenantAccessGuard, partRepository, bomRepository, priceRepository, auditService);
        org.mockito.Mockito.lenient().when(batchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        org.mockito.Mockito.lenient().when(partRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void ingestRequiresRows() {
        IngestRequest empty = new IngestRequest("SAP", ErpBatchType.PART, null, null, List.of(), List.of(), List.of());
        assertThrows(BusinessRuleViolationException.class, () -> service.ingest(empty));
    }

    @Test
    void ingestPersistsBatchAndParts() {
        PartRow part = new PartRow("EXT-1", "PN-100", "A", "Bracket", null, "EA", "ST37", "MAKE", "P1", "PROG1", null);
        IngestRequest req = new IngestRequest("SAP", ErpBatchType.PART, "parts.csv", null, List.of(part), null, null);

        var response = service.ingest(req);

        assertEquals("SAP", response.sourceSystem());
        assertEquals(1, response.rowCount());
        verify(batchRepository, atLeastOnce()).save(any());
        verify(partRepository, atLeastOnce()).save(any());
        verify(auditService).recordCreate(eq("ErpInboundBatch"), anyString(), any());
    }
}
