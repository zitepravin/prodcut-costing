package com.automotive.costing.procurement;

import com.automotive.costing.bom.entity.BomNode;
import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.mixedcost.repository.MixedCostVersionRepository;
import com.automotive.costing.procurement.dto.ProcurementDtos.EvaluateRequest;
import com.automotive.costing.procurement.dto.ProcurementDtos.SetResponse;
import com.automotive.costing.procurement.entity.AlternativeType;
import com.automotive.costing.procurement.entity.ProcurementAlternativeLine;
import com.automotive.costing.procurement.entity.ProcurementAlternativeSet;
import com.automotive.costing.procurement.entity.ProcurementSetStatus;
import com.automotive.costing.procurement.repository.ProcurementAlternativeLineRepository;
import com.automotive.costing.procurement.repository.ProcurementAlternativeSetRepository;
import com.automotive.costing.procurement.service.ProcurementAlternativeService;
import com.automotive.costing.rfq.repository.SupplierQuoteRepository;
import com.automotive.costing.shouldcost.entity.ShouldCostBuildUp;
import com.automotive.costing.shouldcost.repository.ShouldCostBuildUpRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcurementAlternativeServiceTest {

    @Mock ProcurementAlternativeSetRepository setRepository;
    @Mock ProcurementAlternativeLineRepository lineRepository;
    @Mock BomNodeRepository bomNodeRepository;
    @Mock ShouldCostBuildUpRepository shouldCostBuildUpRepository;
    @Mock SupplierQuoteRepository quoteRepository;
    @Mock MixedCostVersionRepository mixedCostVersionRepository;

    ProcurementAlternativeService service;
    List<ProcurementAlternativeLine> savedLines = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new ProcurementAlternativeService(
                setRepository, lineRepository, bomNodeRepository,
                shouldCostBuildUpRepository, quoteRepository, mixedCostVersionRepository);
        when(setRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(lineRepository.saveAll(anyList())).thenAnswer(inv -> {
            savedLines.clear();
            savedLines.addAll(inv.getArgument(0));
            return inv.getArgument(0);
        });
        when(lineRepository.findBySetIdOrderBySequenceNoAsc(any())).thenAnswer(inv -> savedLines);
    }

    @Test
    void evaluate_ranksMakeAndSubcontract() {
        BomNode node = new BomNode();
        node.setBomNodeId("n1");
        node.setProgramId("p1");
        when(bomNodeRepository.findByBomNodeId("n1")).thenReturn(Optional.of(node));

        ShouldCostBuildUp sc = new ShouldCostBuildUp();
        sc.setBuildUpId("bu1");
        sc.setTotalCost(new BigDecimal("100"));
        when(shouldCostBuildUpRepository.findByBomNodeIdOrderByCalculatedAtDesc("n1"))
                .thenReturn(List.of(sc));
        when(quoteRepository.findByBomNodeIdAndStatusOrderByUpdatedAtDesc(any(), any()))
                .thenReturn(List.of());
        when(quoteRepository.findByBomNodeIdOrderByUpdatedAtDesc("n1")).thenReturn(List.of());
        when(mixedCostVersionRepository.findByBomNodeIdOrderByCreatedAtDesc("n1")).thenReturn(List.of());

        SetResponse r = service.evaluate(new EvaluateRequest(
                "n1", null, "Test", new BigDecimal("80"), null, "SubCo", null));

        assertEquals(ProcurementSetStatus.EVALUATED, r.status());
        assertEquals("SUBCONTRACT", r.recommendationCode());
        assertTrue(r.recommendationSummary().contains("NOT an automatic award"));
        assertTrue(savedLines.stream().anyMatch(l ->
                l.getAlternativeType() == AlternativeType.MAKE
                        && l.getUnitCost().compareTo(new BigDecimal("100.000000")) == 0));
        assertTrue(savedLines.stream().anyMatch(l ->
                l.getAlternativeType() == AlternativeType.SUBCONTRACT && l.getRankNo() != null && l.getRankNo() == 1));
    }
}
