package com.automotive.costing.platform.access.tenant;

import com.automotive.costing.bom.entity.BomNode;
import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.makevsbuy.entity.MakeVsBuySimulation;
import com.automotive.costing.platform.access.CustomerContext;
import com.automotive.costing.shouldcost.entity.ShouldCostBuildUp;
import com.automotive.costing.shouldcost.repository.ShouldCostBuildUpRepository;
import com.automotive.costing.masterdata.repository.VehicleProgramRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HighRiskFindByIdTenantIsolationTest {

    private static final String TENANT_A = "customer-a";
    private static final String TENANT_B = "customer-b";
    private static final String SHARED_ID = "shared-uuid-0001";

    private final TenantAccessGuard guard = new TenantAccessGuard();

    @BeforeEach
    void setTenantA() {
        CustomerContext.setCustomerId(TENANT_A);
    }

    @AfterEach
    void clear() {
        CustomerContext.clear();
    }

    @Test
    void tenantA_canLoadOwnBomNode() {
        BomNodeRepository repo = mock(BomNodeRepository.class);
        BomNode node = new BomNode();
        node.setBomNodeId(SHARED_ID);
        node.setCustomerId(TENANT_A);
        when(repo.findByCustomerIdAndBomNodeId(TENANT_A, SHARED_ID)).thenReturn(Optional.of(node));
        BomNode loaded = repo.findByCustomerIdAndBomNodeId(guard.requireCustomerId(), SHARED_ID).orElseThrow();
        assertThat(loaded.getCustomerId()).isEqualTo(TENANT_A);
    }

    @Test
    void tenantA_cannotLoadTenantB_bomNodeViaScopedQuery() {
        BomNodeRepository repo = mock(BomNodeRepository.class);
        when(repo.findByCustomerIdAndBomNodeId(TENANT_A, SHARED_ID)).thenReturn(Optional.empty());
        assertThat(repo.findByCustomerIdAndBomNodeId(guard.requireCustomerId(), SHARED_ID)).isEmpty();
    }

    @Test
    void tenantA_assertOwns_rejectsTenantB_simulation() {
        MakeVsBuySimulation sim = new MakeVsBuySimulation();
        sim.setSimulationId(SHARED_ID);
        sim.setCustomerId(TENANT_B);
        assertThatThrownBy(() -> guard.assertOwns(sim.getCustomerId(), "MakeVsBuySimulation", SHARED_ID))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("not found or not accessible");
    }

    @Test
    void tenantA_assertOwns_allowsOwnSimulation() {
        MakeVsBuySimulation sim = new MakeVsBuySimulation();
        sim.setSimulationId(SHARED_ID);
        sim.setCustomerId(TENANT_A);
        guard.assertOwns(sim.getCustomerId(), "MakeVsBuySimulation", SHARED_ID);
    }

    @Test
    void tenantA_cannotLoadTenantB_shouldCostBuildUp() {
        ShouldCostBuildUpRepository repo = mock(ShouldCostBuildUpRepository.class);
        when(repo.findByCustomerIdAndBuildUpId(TENANT_A, SHARED_ID)).thenReturn(Optional.empty());
        assertThat(repo.findByCustomerIdAndBuildUpId(guard.requireCustomerId(), SHARED_ID)).isEmpty();
    }

    @Test
    void tenantA_canLoadOwnShouldCostBuildUp() {
        ShouldCostBuildUpRepository repo = mock(ShouldCostBuildUpRepository.class);
        ShouldCostBuildUp bu = new ShouldCostBuildUp();
        bu.setBuildUpId(SHARED_ID);
        bu.setCustomerId(TENANT_A);
        when(repo.findByCustomerIdAndBuildUpId(TENANT_A, SHARED_ID)).thenReturn(Optional.of(bu));
        assertThat(repo.findByCustomerIdAndBuildUpId(guard.requireCustomerId(), SHARED_ID))
                .isPresent()
                .get()
                .extracting(ShouldCostBuildUp::getCustomerId)
                .isEqualTo(TENANT_A);
    }

    @Test
    void tenantA_cannotLoadTenantB_vehicleProgram() {
        VehicleProgramRepository repo = mock(VehicleProgramRepository.class);
        when(repo.findByCustomerIdAndProgramId(TENANT_A, SHARED_ID)).thenReturn(Optional.empty());
        assertThat(repo.findByCustomerIdAndProgramId(guard.requireCustomerId(), SHARED_ID)).isEmpty();
    }

    @Test
    void tenantB_context_rejectsTenantA_resource() {
        CustomerContext.setCustomerId(TENANT_B);
        MakeVsBuySimulation sim = new MakeVsBuySimulation();
        sim.setSimulationId(SHARED_ID);
        sim.setCustomerId(TENANT_A);
        assertThatThrownBy(() -> guard.assertOwns(sim.getCustomerId(), "MakeVsBuySimulation", SHARED_ID))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void missingTenantContext_isHardFailure() {
        CustomerContext.clear();
        assertThatThrownBy(guard::requireCustomerId)
                .isInstanceOf(BusinessRuleViolationException.class)
                .extracting(ex -> ((BusinessRuleViolationException) ex).getRuleReference())
                .isEqualTo(TenantAccessGuard.RULE_MISSING_CONTEXT);
    }
}
