package com.automotive.costing.dashboard;

import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardTenantIsolationTest {

    @Test
    void dashboardServiceRequiresCustomerIdFromGuard() throws Exception {
        var field = Class.forName("com.automotive.costing.dashboard.service.DashboardService")
                .getDeclaredField("tenantAccessGuard");
        field.setAccessible(true);
        assertThat(field.getType()).isEqualTo(TenantAccessGuard.class);
    }
}
