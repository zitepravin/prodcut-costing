package com.automotive.costing.erp.service;

import com.automotive.costing.bom.entity.BomNode;
import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.erp.dto.ErpDtos.PromoteResult;
import com.automotive.costing.erp.entity.ErpBatchStatus;
import com.automotive.costing.erp.entity.ErpBatchType;
import com.automotive.costing.erp.entity.ErpBomStaging;
import com.automotive.costing.erp.entity.ErpInboundBatch;
import com.automotive.costing.erp.entity.ErpMaterialPriceStaging;
import com.automotive.costing.erp.entity.ErpStagingRowStatus;
import com.automotive.costing.erp.repository.ErpBomStagingRepository;
import com.automotive.costing.erp.repository.ErpInboundBatchRepository;
import com.automotive.costing.erp.repository.ErpMaterialPriceStagingRepository;
import com.automotive.costing.erp.repository.ErpPartStagingRepository;
import com.automotive.costing.masterdata.entity.VehicleProgram;
import com.automotive.costing.masterdata.repository.MaterialMasterRepository;
import com.automotive.costing.masterdata.repository.VehicleProgramRepository;
import com.automotive.costing.masterdata.service.MaterialMasterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class ErpPromoteServiceTest {

    @Mock ErpInboundBatchRepository batchRepository;
    @Mock private TenantAccessGuard tenantAccessGuard;

    @org.junit.jupiter.api.BeforeEach
    void stubTenant() {
        org.mockito.Mockito.lenient().when(tenantAccessGuard.requireCustomerId()).thenReturn("cust-test-1");
    }
    @Mock ErpPartStagingRepository partRepository;
    @Mock ErpBomStagingRepository bomRepository;
    @Mock ErpMaterialPriceStagingRepository priceRepository;
    @Mock MaterialMasterRepository materialMasterRepository;
    @Mock MaterialMasterService materialMasterService;
    @Mock VehicleProgramRepository vehicleProgramRepository;
    @Mock BomNodeRepository bomNodeRepository;
    @Mock AuditService auditService;

    ErpPromoteService service;

    @BeforeEach
    void setUp() {
        service = new ErpPromoteService(batchRepository, partRepository, bomRepository, priceRepository, materialMasterRepository, materialMasterService, tenantAccessGuard, vehicleProgramRepository, bomNodeRepository, auditService);
    }

    @Test
    @DisplayName("Reject promote when batch is not READY")
    void promoteRejectsNonReady() {
        ErpInboundBatch batch = readyBatch();
        batch.setStatus(ErpBatchStatus.RECEIVED);
        when(batchRepository.findByCustomerIdAndBatchId("cust-test-1", "b1")).thenReturn(Optional.of(batch));

        assertThrows(BusinessRuleViolationException.class, () -> service.promote("b1"));
        verify(materialMasterService, never()).createNewVersion(anyString(), any());
    }

    @Test
    @DisplayName("Promote VALID material price when material exists")
    void promoteMaterialPriceWhenExists() {
        ErpInboundBatch batch = readyBatch();
        when(batchRepository.findByCustomerIdAndBatchId("cust-test-1", "b1")).thenReturn(Optional.of(batch));
        when(batchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ErpMaterialPriceStaging price = new ErpMaterialPriceStaging();
        price.setStagingId("p1");
        price.setBatchId("b1");
        price.setMaterialCode("ST37");
        price.setUnitPrice(new BigDecimal("1.25"));
        price.setCurrencyCode("USD");
        price.setEffectiveFrom(LocalDate.of(2026, 8, 1));
        price.setStatus(ErpStagingRowStatus.VALID);

        when(priceRepository.findByBatchIdOrderByCreatedAtAsc("b1")).thenReturn(List.of(price));
        when(partRepository.findByBatchIdOrderByCreatedAtAsc("b1")).thenReturn(List.of());
        when(bomRepository.findByBatchIdOrderByLineNumberAsc("b1")).thenReturn(List.of());
        when(materialMasterRepository.existsByCustomerIdAndCode("cust-test-1", "ST37")).thenReturn(true);
        when(priceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PromoteResult result = service.promote("b1");

        assertEquals(ErpBatchStatus.PROMOTED, result.status());
        assertEquals(1, result.materialPricesPromoted());
        assertEquals(0, result.materialPricesSkipped());
        assertEquals(0, result.bomLinesPromoted());
        verify(materialMasterService).createNewVersion(eq("ST37"), any());
        verify(auditService).recordUpdate(eq("ErpInboundBatch"), eq("b1"), any(), any());
    }

    @Test
    @DisplayName("Skip material price when code not in master")
    void skipUnknownMaterial() {
        ErpInboundBatch batch = readyBatch();
        when(batchRepository.findByCustomerIdAndBatchId("cust-test-1", "b1")).thenReturn(Optional.of(batch));
        when(batchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ErpMaterialPriceStaging price = new ErpMaterialPriceStaging();
        price.setStagingId("p2");
        price.setBatchId("b1");
        price.setMaterialCode("UNKNOWN");
        price.setUnitPrice(BigDecimal.ONE);
        price.setCurrencyCode("USD");
        price.setEffectiveFrom(LocalDate.of(2026, 8, 1));
        price.setStatus(ErpStagingRowStatus.VALID);

        when(priceRepository.findByBatchIdOrderByCreatedAtAsc("b1")).thenReturn(List.of(price));
        when(partRepository.findByBatchIdOrderByCreatedAtAsc("b1")).thenReturn(List.of());
        when(bomRepository.findByBatchIdOrderByLineNumberAsc("b1")).thenReturn(List.of());
        when(materialMasterRepository.existsByCustomerIdAndCode("cust-test-1", "UNKNOWN")).thenReturn(false);
        when(priceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PromoteResult result = service.promote("b1");

        assertEquals(0, result.materialPricesPromoted());
        assertEquals(1, result.materialPricesSkipped());
        verify(materialMasterService, never()).createNewVersion(anyString(), any());
    }

    @Test
    @DisplayName("Promote BOM line when program_code resolves")
    void promoteBomWhenProgramExists() {
        ErpInboundBatch batch = readyBatch();
        batch.setBatchType(ErpBatchType.BOM);
        when(batchRepository.findByCustomerIdAndBatchId("cust-test-1", "b1")).thenReturn(Optional.of(batch));
        when(batchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(priceRepository.findByBatchIdOrderByCreatedAtAsc("b1")).thenReturn(List.of());
        when(partRepository.findByBatchIdOrderByCreatedAtAsc("b1")).thenReturn(List.of());

        ErpBomStaging bom = new ErpBomStaging();
        bom.setStagingId("bom1");
        bom.setBatchId("b1");
        bom.setParentPartNumber("ASM-ROOT");
        bom.setChildPartNumber("CHILD-1");
        bom.setQuantityPer(new BigDecimal("2"));
        bom.setUom("EA");
        bom.setLineNumber(10);
        bom.setProgramCode("EV-CX-2027");
        bom.setStatus(ErpStagingRowStatus.VALID);

        when(bomRepository.findByBatchIdOrderByLineNumberAsc("b1")).thenReturn(List.of(bom));
        when(bomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VehicleProgram program = new VehicleProgram();
        program.setProgramId("prog-1");
        program.setProgramCode("EV-CX-2027");
        when(vehicleProgramRepository.findByProgramCode("EV-CX-2027")).thenReturn(Optional.of(program));
        when(bomNodeRepository.findFullTreeByProgram("prog-1")).thenReturn(List.of());
        when(bomNodeRepository.save(any(BomNode.class))).thenAnswer(inv -> inv.getArgument(0));

        PromoteResult result = service.promote("b1");

        assertEquals(1, result.bomLinesPromoted());
        assertEquals(0, result.bomLinesSkipped());
        verify(bomNodeRepository, atLeastOnce()).save(any(BomNode.class));
        ArgumentCaptor<ErpBomStaging> bomCap = ArgumentCaptor.forClass(ErpBomStaging.class);
        verify(bomRepository, atLeastOnce()).save(bomCap.capture());
        assertEquals(ErpStagingRowStatus.PROMOTED, bomCap.getValue().getStatus());
    }

    @Test
    @DisplayName("Skip BOM line when program_code unknown")
    void skipBomWhenProgramMissing() {
        ErpInboundBatch batch = readyBatch();
        when(batchRepository.findByCustomerIdAndBatchId("cust-test-1", "b1")).thenReturn(Optional.of(batch));
        when(batchRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(priceRepository.findByBatchIdOrderByCreatedAtAsc("b1")).thenReturn(List.of());
        when(partRepository.findByBatchIdOrderByCreatedAtAsc("b1")).thenReturn(List.of());

        ErpBomStaging bom = new ErpBomStaging();
        bom.setStagingId("bom2");
        bom.setBatchId("b1");
        bom.setParentPartNumber("P");
        bom.setChildPartNumber("C");
        bom.setQuantityPer(BigDecimal.ONE);
        bom.setProgramCode("NO-SUCH");
        bom.setStatus(ErpStagingRowStatus.VALID);

        when(bomRepository.findByBatchIdOrderByLineNumberAsc("b1")).thenReturn(List.of(bom));
        when(bomRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(vehicleProgramRepository.findByProgramCode("NO-SUCH")).thenReturn(Optional.empty());

        PromoteResult result = service.promote("b1");

        assertEquals(0, result.bomLinesPromoted());
        assertEquals(1, result.bomLinesSkipped());
        verify(bomNodeRepository, never()).save(any());
    }

    private ErpInboundBatch readyBatch() {
        ErpInboundBatch batch = new ErpInboundBatch();
        batch.setBatchId("b1");
        batch.setSourceSystem("SAP");
        batch.setBatchType(ErpBatchType.MATERIAL_PRICE);
        batch.setStatus(ErpBatchStatus.READY);
        batch.setRowCount(1);
        batch.setErrorCount(0);
        batch.setReceivedAt(Instant.now());
        batch.setCreatedAt(Instant.now());
        return batch;
    }
}
