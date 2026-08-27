package com.automotive.costing.dashboard;

import com.automotive.costing.dashboard.controller.DashboardController;
import com.automotive.costing.dashboard.dto.DashboardDtos.KpiResponse;
import com.automotive.costing.dashboard.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock private DashboardService dashboardService;
    @InjectMocks private DashboardController controller;

    @Test
    void kpis_returns200PayloadFromService() {
        KpiResponse body = new KpiResponse(
                1, 2, 3, 4, 5, 6, 7, 8, 9,
                0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0,
                11, 2, 3, 4, 1, 5, 2,
                new BigDecimal("10.00"),
                1, 2, 3, 4, 5,
                new BigDecimal("20.00"));
        when(dashboardService.kpis()).thenReturn(body);

        ResponseEntity<KpiResponse> res = controller.kpis();
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().openRfqs()).isEqualTo(11L);
        assertThat(res.getBody().purchasingSpend()).isEqualByComparingTo("10.00");
    }
}
