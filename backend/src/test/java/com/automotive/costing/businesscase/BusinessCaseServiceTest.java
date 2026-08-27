package com.automotive.costing.businesscase;

import com.automotive.costing.businesscase.dto.BusinessCaseDtos.CreateScenarioRequest;
import com.automotive.costing.businesscase.dto.BusinessCaseDtos.ScenarioResponse;
import com.automotive.costing.businesscase.entity.BusinessCaseScenario;
import com.automotive.costing.businesscase.entity.BusinessCaseStatus;
import com.automotive.costing.businesscase.repository.BusinessCaseScenarioRepository;
import com.automotive.costing.businesscase.service.BusinessCaseService;
import com.automotive.costing.masterdata.repository.VehicleProgramRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BusinessCaseServiceTest {

    @Mock BusinessCaseScenarioRepository repository;
    @Mock VehicleProgramRepository vehicleProgramRepository;
    BusinessCaseService service;

    @BeforeEach
    void setUp() {
        service = new BusinessCaseService(repository, vehicleProgramRepository);
    }

    @Test
    void calculate_contributionAndNpv() {
        String id = UUID.randomUUID().toString();
        BusinessCaseScenario s = new BusinessCaseScenario();
        s.setScenarioId(id);
        s.setProgramId("prog-1");
        s.setScenarioCode("BASE");
        s.setScenarioName("Base");
        s.setUnitSellingPrice(new BigDecimal("100.000000"));
        s.setUnitShouldCost(new BigDecimal("70.000000"));
        s.setUnitVariableCost(new BigDecimal("70.000000"));
        s.setAnnualVolume(new BigDecimal("1000.000000"));
        s.setHorizonYears(2);
        s.setDiscountRatePct(new BigDecimal("0.000000")); // simplify NPV = sum annual_net - inv
        s.setInitialInvestment(new BigDecimal("20000.000000"));
        s.setFixedAnnualCost(new BigDecimal("0.000000"));
        s.setStatus(BusinessCaseStatus.DRAFT);

        when(repository.findById(id)).thenReturn(Optional.of(s));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ScenarioResponse r = service.calculate(id);
        assertEquals(0, new BigDecimal("30.000000").compareTo(r.contributionMarginUnit()));
        assertEquals(0, new BigDecimal("30000.000000").compareTo(r.annualContribution()));
        // NPV = -20000 + 30000 + 30000 = 40000 at 0% discount
        assertEquals(0, new BigDecimal("40000.000000").compareTo(r.npvAmount()));
        assertEquals(BusinessCaseStatus.CALCULATED, r.status());
        assertNotNull(r.calculationBasis());
        assertTrue(r.calculationBasis().contains("Not ERP P&L"));
    }

    @Test
    void create_requiresProgram() {
        when(vehicleProgramRepository.existsById("missing")).thenReturn(false);
        CreateScenarioRequest req = new CreateScenarioRequest(
                "missing", "A", "Name", "USD",
                new BigDecimal("1"), new BigDecimal("1"), null,
                new BigDecimal("1"), 5, null, null, null);
        assertThrows(Exception.class, () -> service.create(req));
    }
}
