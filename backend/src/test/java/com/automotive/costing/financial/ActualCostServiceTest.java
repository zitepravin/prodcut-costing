package com.automotive.costing.financial;
import com.automotive.costing.bom.entity.BomNode;
import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.financial.dto.FinancialDtos.ImportActualRequest;
import com.automotive.costing.financial.repository.ActualCostRecordRepository;
import com.automotive.costing.financial.service.ActualCostService;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
class ActualCostServiceTest {
    @Mock ActualCostRecordRepository repository;
    @Mock BomNodeRepository bomNodeRepository;
    @Mock TenantAccessGuard tenantAccessGuard;
    @InjectMocks ActualCostService service;
    @BeforeEach void setUp() { when(tenantAccessGuard.requireCustomerId()).thenReturn("cust-test"); }
    @Test void import_requiresRealUnitCost() {
        BomNode n = new BomNode(); n.setBomNodeId("n1"); n.setProgramId("p1");
        when(bomNodeRepository.findByBomNodeId("n1")).thenReturn(Optional.of(n));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var r = service.importActual(new ImportActualRequest(
                "n1", null, "IMPORT", "PO-1", "2026-08", new BigDecimal("10"),
                new BigDecimal("12.5"), null, "USD", null, null, null, null, null, null));
        assertEquals(0, new BigDecimal("12.500000").compareTo(r.unitCost()));
        assertEquals(0, new BigDecimal("125.000000").compareTo(r.totalCost()));
        assertEquals("IMPORT", r.sourceSystem());
    }
}
