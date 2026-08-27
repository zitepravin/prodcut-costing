package com.automotive.costing.dashboard;

import com.automotive.costing.dashboard.controller.ProgramCostPositionController;
import com.automotive.costing.platform.access.RequiresModule;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardEntitlementTest {

    @Test
    void programCostPositionRequiresProductCostingModule() {
        RequiresModule mod = ProgramCostPositionController.class.getAnnotation(RequiresModule.class);
        assertThat(mod).isNotNull();
        assertThat(mod.value()).isEqualTo("PRODUCT_COSTING");
    }

    @Test
    void programCostPositionRequiresCostingPermission() throws Exception {
        Method m = ProgramCostPositionController.class.getMethod(
                "costPosition", String.class, String.class, boolean.class);
        PreAuthorize pre = m.getAnnotation(PreAuthorize.class);
        assertThat(pre).isNotNull();
        assertThat(pre.value()).contains("COSTING_VIEW");
        assertThat(pre.value()).contains("SHOULD_COST");
    }
}
