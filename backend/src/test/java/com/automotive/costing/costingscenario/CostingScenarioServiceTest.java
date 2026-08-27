package com.automotive.costing.costingscenario;

import com.automotive.costing.bom.entity.BomNode;
import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.costingscenario.dto.CostingScenarioDtos.*;
import com.automotive.costing.costingscenario.entity.*;
import com.automotive.costing.costingscenario.repository.AdditiveCostLineRepository;
import com.automotive.costing.costingscenario.repository.CostingScenarioRepository;
import com.automotive.costing.costingscenario.service.CostingScenarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CostingScenarioServiceTest {

    @Mock CostingScenarioRepository scenarioRepository;
    @Mock AdditiveCostLineRepository additiveRepository;
    @Mock BomNodeRepository bomNodeRepository;
    CostingScenarioService service;

    @BeforeEach
    void setUp() {
        service = new CostingScenarioService(scenarioRepository, additiveRepository, bomNodeRepository);
    }

    @Test
    void approve_locksScenario() {
        CostingScenario s = baseScenario();
        when(scenarioRepository.findById("s1")).thenReturn(Optional.of(s));
        when(scenarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(additiveRepository.findByScenarioIdOrderByLineCodeAsc("s1")).thenReturn(List.of());

        ScenarioResponse r = service.approve("s1");
        assertEquals(ScenarioStatus.APPROVED, r.status());
        assertTrue(r.locked());
    }

    @Test
    void addAdditive_blockedWhenApproved() {
        CostingScenario s = baseScenario();
        s.setStatus(ScenarioStatus.APPROVED);
        s.setLocked(true);
        when(scenarioRepository.findById("s1")).thenReturn(Optional.of(s));

        assertThrows(BusinessRuleViolationException.class, () ->
                service.addAdditiveLine("s1", new AddAdditiveLineRequest(
                        "P1", "Prototype", AdditiveCostType.PROTOTYPE,
                        new BigDecimal("1000"), "USD", false, null, null)));
    }

    @Test
    void additive_amortizedPerUnit() {
        CostingScenario s = baseScenario();
        when(scenarioRepository.findById("s1")).thenReturn(Optional.of(s));
        when(additiveRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AdditiveLineResponse line = service.addAdditiveLine("s1", new AddAdditiveLineRequest(
                "ST1", "Special tooling", AdditiveCostType.SPECIAL_TOOLING,
                new BigDecimal("10000"), "USD", true, new BigDecimal("1000"), null));

        assertEquals(0, new BigDecimal("10.000000").compareTo(line.amountPerUnit()));
        assertTrue(line.amortized());
    }

    @Test
    void branch_createsDraftChild() {
        CostingScenario source = baseScenario();
        source.setStatus(ScenarioStatus.APPROVED);
        source.setLocked(true);
        when(scenarioRepository.findById("s1")).thenReturn(Optional.of(source));
        when(scenarioRepository.findByBomNodeIdAndScenarioCode("n1", "ECN1")).thenReturn(Optional.empty());
        when(scenarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(additiveRepository.findByScenarioIdOrderByLineCodeAsc(any())).thenReturn(List.of());

        ScenarioResponse r = service.branch(new BranchScenarioRequest(
                "s1", "ECN1", "Engineering change", ScenarioType.ENGINEERING_CHANGE, "from approved"));
        assertEquals(ScenarioStatus.DRAFT, r.status());
        assertEquals("s1", r.parentScenarioId());
        assertFalse(r.locked());
    }

    private CostingScenario baseScenario() {
        CostingScenario s = new CostingScenario();
        s.setScenarioId("s1");
        s.setBomNodeId("n1");
        s.setProgramId("p1");
        s.setScenarioCode("BASE");
        s.setScenarioName("Base");
        s.setScenarioType(ScenarioType.BASE);
        s.setStatus(ScenarioStatus.DRAFT);
        s.setLocked(false);
        return s;
    }
}
