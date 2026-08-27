package com.automotive.costing.standardcost;

import com.automotive.costing.bom.entity.BomNode;
import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.shouldcost.entity.ShouldCostBuildUp;
import com.automotive.costing.shouldcost.repository.ShouldCostBuildUpRepository;
import com.automotive.costing.standardcost.dto.StandardCostDtos.CreateFromShouldCostRequest;
import com.automotive.costing.standardcost.dto.StandardCostDtos.StandardCostResponse;
import com.automotive.costing.standardcost.entity.StandardCost;
import com.automotive.costing.standardcost.entity.StandardCostStatus;
import com.automotive.costing.standardcost.repository.StandardCostRepository;
import com.automotive.costing.standardcost.service.StandardCostService;
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
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class StandardCostServiceTest {

    @Mock StandardCostRepository repository;
    @Mock ShouldCostBuildUpRepository shouldCostBuildUpRepository;
    @Mock BomNodeRepository bomNodeRepository;
    StandardCostService service;

    @BeforeEach
    void setUp() {
        service = new StandardCostService(repository, shouldCostBuildUpRepository, bomNodeRepository);
        lenient().when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createFromShouldCost_startsAsDraft() {
        ShouldCostBuildUp sc = new ShouldCostBuildUp();
        sc.setBuildUpId("bu1");
        sc.setBomNodeId("n1");
        sc.setTotalCost(new BigDecimal("125.50"));
        sc.setMaterialCost(new BigDecimal("50"));
        when(shouldCostBuildUpRepository.findById("bu1")).thenReturn(Optional.of(sc));
        BomNode node = new BomNode();
        node.setBomNodeId("n1");
        node.setProgramId("p1");
        when(bomNodeRepository.findByBomNodeId("n1")).thenReturn(Optional.of(node));
        when(repository.maxVersionNo("n1")).thenReturn(0);

        StandardCostResponse r = service.createFromShouldCost(
                new CreateFromShouldCostRequest("bu1", "PLANT1", null, null, null));

        assertEquals(StandardCostStatus.DRAFT, r.status());
        assertEquals(1, r.versionNo());
        assertEquals(0, new BigDecimal("125.500000").compareTo(r.unitCost()));
        assertEquals("SHOULD_COST", r.sourceType());
        assertEquals("bu1", r.sourceRefId());
    }

    @Test
    void release_supersedesPreviousReleased() {
        StandardCost draft = cost("sc2", "n1", 2, StandardCostStatus.DRAFT);
        StandardCost prev = cost("sc1", "n1", 1, StandardCostStatus.RELEASED);
        when(repository.findById("sc2")).thenReturn(Optional.of(draft));
        when(repository.findByBomNodeIdAndStatusIn("n1", List.of(StandardCostStatus.RELEASED)))
                .thenReturn(new ArrayList<>(List.of(prev)));

        StandardCostResponse r = service.release("sc2", "manager1");
        assertEquals(StandardCostStatus.RELEASED, r.status());
        assertEquals("manager1", r.releasedBy());
        assertEquals(StandardCostStatus.SUPERSEDED, prev.getStatus());
        assertEquals("sc2", prev.getSupersededById());
    }

    @Test
    void freeze_requiresReleased() {
        StandardCost draft = cost("sc1", "n1", 1, StandardCostStatus.DRAFT);
        when(repository.findById("sc1")).thenReturn(Optional.of(draft));
        assertThrows(BusinessRuleViolationException.class, () -> service.freeze("sc1", "u1"));
    }

    private static StandardCost cost(String id, String bom, int ver, StandardCostStatus st) {
        StandardCost s = new StandardCost();
        s.setStandardCostId(id);
        s.setBomNodeId(bom);
        s.setVersionNo(ver);
        s.setStatus(st);
        s.setUnitCost(new BigDecimal("100"));
        s.setCurrencyCode("USD");
        s.setSourceType("SHOULD_COST");
        return s;
    }
}
