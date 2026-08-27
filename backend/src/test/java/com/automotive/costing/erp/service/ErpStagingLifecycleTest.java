package com.automotive.costing.erp.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.erp.dto.ErpDtos.BatchResponse;
import com.automotive.costing.erp.entity.ErpBatchStatus;
import com.automotive.costing.erp.entity.ErpBatchType;
import com.automotive.costing.erp.entity.ErpInboundBatch;
import com.automotive.costing.erp.entity.ErpMaterialPriceStaging;
import com.automotive.costing.erp.entity.ErpStagingRowStatus;
import com.automotive.costing.erp.repository.ErpBomStagingRepository;
import com.automotive.costing.erp.repository.ErpInboundBatchRepository;
import com.automotive.costing.erp.repository.ErpMaterialPriceStagingRepository;
import com.automotive.costing.erp.repository.ErpPartStagingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ErpStagingLifecycleTest {

    @Mock ErpInboundBatchRepository batchRepository;
    @Mock private TenantAccessGuard tenantAccessGuard;

    @org.junit.jupiter.api.BeforeEach
    void stubTenant() {
        org.mockito.Mockito.lenient().when(tenantAccessGuard.requireCustomerId()).thenReturn("cust-test-1");
    }
    @Mock ErpPartStagingRepository partRepository;
    @Mock ErpBomStagingRepository bomRepository;
    @Mock ErpMaterialPriceStagingRepository priceRepository;
    @Mock AuditService auditService;

    ErpStagingService service;

    @BeforeEach
    void setUp() {
        service = new ErpStagingService(batchRepository, tenantAccessGuard, partRepository, bomRepository, priceRepository, auditService);
    }

    @Test
    @DisplayName("Cancel READY batch")
    void cancelReady() {
        ErpInboundBatch batch = batch(ErpBatchStatus.READY);
        when(batchRepository.findByCustomerIdAndBatchId("cust-test-1", "b1")).thenReturn(Optional.of(batch));
        when(batchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BatchResponse result = service.cancel("b1");

        assertEquals(ErpBatchStatus.CANCELLED, result.status());
        verify(auditService).recordUpdate(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Cannot cancel PROMOTED batch")
    void cancelPromotedRejected() {
        when(batchRepository.findByCustomerIdAndBatchId("cust-test-1", "b1"))
                .thenReturn(Optional.of(batch(ErpBatchStatus.PROMOTED)));
        assertThrows(BusinessRuleViolationException.class, () -> service.cancel("b1"));
    }

    @Test
    @DisplayName("Reopen FAILED batch resets rows to PENDING")
    void reopenFailed() {
        ErpInboundBatch batch = batch(ErpBatchStatus.FAILED);
        when(batchRepository.findByCustomerIdAndBatchId("cust-test-1", "b1")).thenReturn(Optional.of(batch));
        when(batchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ErpMaterialPriceStaging price = new ErpMaterialPriceStaging();
        price.setStagingId("p1");
        price.setStatus(ErpStagingRowStatus.INVALID);
        price.setErrorMessage("bad");
        when(partRepository.findByBatchIdOrderByCreatedAtAsc("b1")).thenReturn(List.of());
        when(bomRepository.findByBatchIdOrderByLineNumberAsc("b1")).thenReturn(List.of());
        when(priceRepository.findByBatchIdOrderByCreatedAtAsc("b1")).thenReturn(List.of(price));
        when(priceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BatchResponse result = service.reopen("b1");

        assertEquals(ErpBatchStatus.RECEIVED, result.status());
        assertEquals(ErpStagingRowStatus.PENDING, price.getStatus());
        assertEquals(null, price.getErrorMessage());
    }

    @Test
    @DisplayName("Reopen READY rejected")
    void reopenReadyRejected() {
        when(batchRepository.findByCustomerIdAndBatchId("cust-test-1", "b1"))
                .thenReturn(Optional.of(batch(ErpBatchStatus.READY)));
        assertThrows(BusinessRuleViolationException.class, () -> service.reopen("b1"));
    }

    private ErpInboundBatch batch(ErpBatchStatus status) {
        ErpInboundBatch b = new ErpInboundBatch();
        b.setBatchId("b1");
        b.setSourceSystem("SAP");
        b.setBatchType(ErpBatchType.MATERIAL_PRICE);
        b.setStatus(status);
        b.setRowCount(1);
        b.setErrorCount(0);
        b.setReceivedAt(Instant.now());
        b.setCreatedAt(Instant.now());
        return b;
    }
}
