package com.automotive.costing.dashboard;

import com.automotive.costing.dashboard.controller.DashboardController;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardPermissionTest {

    @Test
    void kpisEndpointRequiresDashboardViewPermission() throws Exception {
        Method m = DashboardController.class.getMethod("kpis");
        PreAuthorize pre = m.getAnnotation(PreAuthorize.class);
        assertThat(pre).isNotNull();
        assertThat(pre.value()).contains("permissionAuth.has('DASHBOARD_VIEW')");
    }
}
