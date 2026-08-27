package com.automotive.costing.coststructure;

import com.automotive.costing.bom.entity.BomNode;
import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.coststructure.dto.CostStructureDtos.*;
import com.automotive.costing.coststructure.entity.CostEstimateItem;
import com.automotive.costing.coststructure.repository.CostEstimateItemRepository;
import com.automotive.costing.coststructure.service.CostComponentStructureService;
import com.automotive.costing.coststructure.service.CostEstimateItemizationService;
import com.automotive.costing.coststructure.service.MultilevelCostedBomService;
import com.automotive.costing.shouldcost.entity.CostComponentDefinition;
import com.automotive.costing.shouldcost.entity.ShouldCostBuildUp;
import com.automotive.costing.shouldcost.repository.CostComponentDefinitionRepository;
import com.automotive.costing.shouldcost.repository.ShouldCostBuildUpRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CostStructureServicesTest {

    @Mock CostComponentDefinitionRepository componentRepo;
    @Mock BomNodeRepository bomNodeRepository;
    @Mock ShouldCostBuildUpRepository shouldCostBuildUpRepository;
    @Mock CostEstimateItemRepository itemRepository;

    @Test
    void a3_hierarchyBuildsParentChild() {
        CostComponentDefinition material = def("MATERIAL", null, 10);
        CostComponentDefinition raw = def("RAW_MATERIAL", "MATERIAL", 11);
        when(componentRepo.findAll()).thenReturn(List.of(material, raw));

        ComponentStructureResponse r = new CostComponentStructureService(componentRepo).hierarchy();
        assertEquals(1, r.roots().size());
        assertEquals("MATERIAL", r.roots().get(0).componentCode());
        assertEquals(1, r.roots().get(0).children().size());
        assertEquals("RAW_MATERIAL", r.roots().get(0).children().get(0).componentCode());
    }

    @Test
    void a4_costedBomUsesShouldCost() {
        BomNode root = new BomNode();
        root.setBomNodeId("root");
        root.setProgramId("p1");
        root.setPartNumber("P-1");
        root.setQuantityPer(BigDecimal.ONE);

        ShouldCostBuildUp sc = new ShouldCostBuildUp();
        sc.setBuildUpId("bu1");
        sc.setTotalCost(new BigDecimal("100"));
        sc.setMaterialCost(new BigDecimal("40"));
        sc.setProcessCost(new BigDecimal("30"));
        sc.setToolingCost(new BigDecimal("10"));
        sc.setPackagingFreightCost(new BigDecimal("5"));
        sc.setOverheadCost(new BigDecimal("15"));

        when(bomNodeRepository.findByBomNodeId("root")).thenReturn(Optional.of(root));
        when(bomNodeRepository.findSubtree("root")).thenReturn(List.of(root));
        when(shouldCostBuildUpRepository.findByBomNodeIdOrderByCalculatedAtDesc("root"))
                .thenReturn(List.of(sc));

        MultilevelCostedBomResponse r = new MultilevelCostedBomService(bomNodeRepository, shouldCostBuildUpRepository)
                .costedTree("root");
        assertEquals(0, new BigDecimal("100.000000").compareTo(r.tree().unitCost()));
        assertTrue(r.tree().costSource().startsWith("SHOULD_COST"));
        assertEquals(0, new BigDecimal("40.000000").compareTo(r.tree().materialCost()));
    }

    @Test
    void a5_itemizationMaterializesComponents() {
        ShouldCostBuildUp sc = new ShouldCostBuildUp();
        sc.setBuildUpId("bu1");
        sc.setBomNodeId("n1");
        sc.setMaterialCost(new BigDecimal("10"));
        sc.setProcessCost(new BigDecimal("20"));
        sc.setToolingCost(new BigDecimal("5"));
        sc.setPackagingFreightCost(BigDecimal.ZERO);
        sc.setOverheadCost(new BigDecimal("3"));
        sc.setSgaCost(new BigDecimal("2"));
        sc.setProfitMargin(new BigDecimal("1"));

        when(shouldCostBuildUpRepository.findById("bu1")).thenReturn(Optional.of(sc));
        when(itemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        ItemizationResponse r = new CostEstimateItemizationService(itemRepository, shouldCostBuildUpRepository)
                .materializeFromShouldCost("bu1");

        assertEquals(7, r.lines().size());
        assertEquals(0, new BigDecimal("41").compareTo(r.totalAmount()));
        assertTrue(r.totalFixed().compareTo(BigDecimal.ZERO) > 0);
        verify(itemRepository).deleteByEstimateRefTypeAndEstimateRefId("SHOULD_COST", "bu1");
    }

    private static CostComponentDefinition def(String code, String parent, int seq) {
        CostComponentDefinition c = new CostComponentDefinition();
        c.setComponentCode(code);
        c.setDisplayName(code);
        c.setParentComponentCode(parent);
        c.setSequenceNo(seq);
        c.setCategory("DIRECT");
        c.setActive(true);
        c.setCostBehavior("VARIABLE");
        c.setHierarchyLevel(parent == null ? 1 : 2);
        return c;
    }
}
