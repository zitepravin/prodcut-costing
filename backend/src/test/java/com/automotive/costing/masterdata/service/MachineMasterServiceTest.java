package com.automotive.costing.masterdata.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.platform.user.service.AccessScopeService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.masterdata.dto.MachineMasterDtos.CreateRequest;
import com.automotive.costing.masterdata.dto.MachineMasterDtos.Response;
import com.automotive.costing.masterdata.dto.MachineMasterDtos.SimulationRequest;
import com.automotive.costing.masterdata.dto.MachineMasterDtos.SimulationResponse;
import com.automotive.costing.masterdata.entity.MachineMaster;
import com.automotive.costing.masterdata.mapper.MachineMasterMapper;
import com.automotive.costing.masterdata.mapper.MachineMasterMapperImpl;
import com.automotive.costing.masterdata.repository.MachineMasterRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.platform.access.CustomerContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for OHD-01 (machine hourly rate build-up), mapped to QA_Test_Case_Tracker.xlsx
 * Module M05 (TC-0065 range). Verifies the composed rate formula and the divide-by-zero
 * guard mandated by TDD Section 6.2.
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class MachineMasterServiceTest {

    @Mock
    private MachineMasterRepository repository;

    @Mock
    private AuditService auditService;

    @Mock
    private TenantAccessGuard tenantAccessGuard;
    @Mock
    private AccessScopeService accessScopeService;

    private MachineMasterMapper mapper;
    private MachineMasterService service;

    @AfterEach
    void tearDown() {
        CustomerContext.clear();
    }

    @BeforeEach
    void setUp() {
        CustomerContext.setCustomerId("test-customer");
        lenient().when(tenantAccessGuard.requireCustomerId()).thenReturn("test-customer");
        mapper = new MachineMasterMapperImpl();
        service = new MachineMasterService(repository, mapper, auditService, accessScopeService, tenantAccessGuard);
    }

    // Corresponds to TC-0065 (M05 / OHD-01, Positive): rate build-up computed correctly.
    @Test
    void create_computesHourlyRateFromComponents() {
        when(repository.existsByCode("MC-PRESS-001")).thenReturn(false);
        when(repository.save(any(MachineMaster.class))).thenAnswer(inv -> inv.getArgument(0));

        // capitalCost=1,000,000 / depreciationLife=10yr / annualOperatingHours=2000hr = 50.0/hr
        // + power 12.5 + maintenance 8.0 + floorSpace 4.5 = 75.0/hr
        CreateRequest request = new CreateRequest(
                "MC-PRESS-001", "400T Stamping Press", null, "HR", null,
                LocalDate.of(2026, 1, 1), null,
                "Stamping Press", "400 Ton",
                new BigDecimal("10"), new BigDecimal("1000000"), new BigDecimal("2000"),
                new BigDecimal("12.5"), new BigDecimal("8.0"), new BigDecimal("4.5"), "2-shift");

        Response result = service.create(request);

        assertThat(result.rate()).isEqualByComparingTo("75.000000");
    }

    // Corresponds to TC-0066 (M05 / OHD-01, Negative/Boundary): zero annual operating hours
    // must raise an explicit validation error, never a silent divide-by-zero (TDD Section 6.2).
    @Test
    void create_withZeroAnnualOperatingHours_throwsBusinessRuleViolation() {
        when(repository.existsByCode("MC-PRESS-002")).thenReturn(false);

        CreateRequest request = new CreateRequest(
                "MC-PRESS-002", "Faulty config", null, "HR", null,
                LocalDate.of(2026, 1, 1), null,
                "Stamping Press", "400 Ton",
                new BigDecimal("10"), new BigDecimal("1000000"), BigDecimal.ZERO,
                new BigDecimal("12.5"), new BigDecimal("8.0"), new BigDecimal("4.5"), "1-shift");

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("annualOperatingHours must be greater than zero");
    }

    // OHD-06 (Positive): what-if simulation of a capacity-utilization change (annual
    // operating hours 2000 -> 4000, i.e. adding a second shift) halves the depreciation
    // component without persisting anything.
    @Test
    void simulate_withIncreasedAnnualOperatingHours_recomputesRateWithoutPersisting() {
        MachineMaster existing = new MachineMaster();
        existing.setRecordId("rec-1");
        existing.setCode("MC-PRESS-001");
        existing.setUom("HR");
        existing.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        existing.setRate(new BigDecimal("75.000000"));
        existing.setDepreciationLifeYears(new BigDecimal("10"));
        existing.setCapitalCost(new BigDecimal("1000000"));
        existing.setAnnualOperatingHours(new BigDecimal("2000"));
        existing.setPowerCostPerHour(new BigDecimal("12.5"));
        existing.setMaintenanceCostPerHour(new BigDecimal("8.0"));
        existing.setFloorSpaceCostPerHour(new BigDecimal("4.5"));

        when(repository.findByCustomerIdAndRecordId("test-customer", "rec-1")).thenReturn(java.util.Optional.of(existing));

        SimulationRequest request = new SimulationRequest(null, null, new BigDecimal("4000"), null, null, null);

        SimulationResponse result = service.simulate("rec-1", request);

        // depreciation component: 1,000,000 / 10 / 4000 = 25.0/hr; + 12.5 + 8.0 + 4.5 = 50.0/hr
        assertThat(result.simulatedHourlyRate()).isEqualByComparingTo("50.000000");
        assertThat(result.currentHourlyRate()).isEqualByComparingTo("75.000000");
        assertThat(result.delta()).isEqualByComparingTo("-25.000000");
        verify(repository, never()).save(any());
    }
}
