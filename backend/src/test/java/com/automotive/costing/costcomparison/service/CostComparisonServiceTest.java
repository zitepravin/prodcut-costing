package com.automotive.costing.costcomparison.service;

import com.automotive.costing.costcomparison.dto.CostComparisonDtos.WorkspaceResponse;
import com.automotive.costing.rfq.repository.SupplierQuoteRepository;
import com.automotive.costing.shouldcost.repository.ShouldCostBuildUpRepository;
import com.automotive.costing.targetcosting.repository.TargetCostRecordRepository;
import com.automotive.costing.tooling.repository.ToolingCostCalculationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CostComparisonServiceTest {

    @Mock ShouldCostBuildUpRepository shouldCostBuildUpRepository;
    @Mock SupplierQuoteRepository supplierQuoteRepository;
    @Mock ToolingCostCalculationRepository toolingCostCalculationRepository;
    @Mock TargetCostRecordRepository targetCostRecordRepository;

    CostComparisonService service;

    @BeforeEach
    void setUp() {
        service = new CostComparisonService(
                shouldCostBuildUpRepository,
                supplierQuoteRepository,
                toolingCostCalculationRepository,
                targetCostRecordRepository);
    }

    @Test
    void emptyWorkspaceWhenNoData() {
        when(shouldCostBuildUpRepository.findByBomNodeIdOrderByCalculatedAtDesc("n1")).thenReturn(List.of());
        when(supplierQuoteRepository.findByBomNodeIdOrderByUpdatedAtDesc("n1")).thenReturn(List.of());
        when(toolingCostCalculationRepository.findFirstByBomNodeIdOrderByCalculatedAtDesc("n1"))
                .thenReturn(Optional.empty());
        when(targetCostRecordRepository.findByBomNodeIdOrderByEffectiveFromDesc("n1")).thenReturn(List.of());

        WorkspaceResponse ws = service.workspaceForBomNode("n1");
        assertEquals("n1", ws.bomNodeId());
        assertTrue(ws.shouldCostScenarios().isEmpty());
        assertTrue(ws.supplierQuotes().isEmpty());
        assertTrue(ws.toolingCosts().isEmpty());
        assertTrue(ws.targets().isEmpty());
    }

    @Test
    void requiresBomNodeId() {
        assertThrows(IllegalArgumentException.class, () -> service.workspaceForBomNode(" "));
    }
}
