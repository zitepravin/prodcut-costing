package com.automotive.costing.financial;

import com.automotive.costing.erp.sap.repository.SapCostEstimateStagingRepository;
import com.automotive.costing.financial.dto.FinancialDtos.TriangleCompareResponse;
import com.automotive.costing.financial.entity.ActualCostRecord;
import com.automotive.costing.financial.repository.ActualCostRecordRepository;
import com.automotive.costing.financial.service.CostTriangleService;
import com.automotive.costing.shouldcost.entity.ShouldCostBuildUp;
import com.automotive.costing.shouldcost.repository.ShouldCostBuildUpRepository;
import com.automotive.costing.standardcost.entity.StandardCost;
import com.automotive.costing.standardcost.entity.StandardCostStatus;
import com.automotive.costing.standardcost.repository.StandardCostRepository;
import com.automotive.costing.targetcosting.repository.TargetCostRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CostTriangleServiceTest {

    @Mock StandardCostRepository standardCostRepository;
    @Mock ShouldCostBuildUpRepository shouldCostBuildUpRepository;
    @Mock ActualCostRecordRepository actualCostRecordRepository;
    @Mock TargetCostRecordRepository targetCostRecordRepository;
    @Mock SapCostEstimateStagingRepository sapStagingRepository;

    @InjectMocks CostTriangleService service;

    @Test
    void compare_marksMissingActual_andComputesStdVsShould() {
        StandardCost std = new StandardCost();
        std.setStandardCostId("s1");
        std.setUnitCost(new BigDecimal("100"));
        std.setCurrencyCode("USD");
        std.setVersionNo(1);
        std.setStatus(StandardCostStatus.RELEASED);
        when(standardCostRepository.findFirstByBomNodeIdAndStatusOrderByVersionNoDesc("n1", StandardCostStatus.RELEASED))
                .thenReturn(Optional.of(std));

        ShouldCostBuildUp sc = new ShouldCostBuildUp();
        sc.setBuildUpId("bu1");
        sc.setTotalCost(new BigDecimal("110"));
        when(shouldCostBuildUpRepository.findByBomNodeIdOrderByCalculatedAtDesc("n1"))
                .thenReturn(List.of(sc));

        when(targetCostRecordRepository.findByBomNodeIdOrderByEffectiveFromDesc("n1")).thenReturn(List.of());
        when(actualCostRecordRepository.findFirstByBomNodeIdOrderByImportedAtDesc("n1")).thenReturn(Optional.empty());
        when(sapStagingRepository.findByBomNodeIdOrderByReceivedAtDesc("n1")).thenReturn(List.of());

        TriangleCompareResponse r = service.compare("n1", "USD");
        assertEquals(0, new BigDecimal("-10.000000").compareTo(r.standardVsShould()));
        assertNull(r.standardVsActual());
        assertTrue(r.buckets().stream().anyMatch(b ->
                "ACTUAL".equals(b.bucket()) && "DATA_NOT_AVAILABLE".equals(b.dataStatus())));
        assertTrue(r.summary().contains("no Material Ledger"));
    }
}
