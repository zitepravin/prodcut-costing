package com.automotive.costing.dashboard.controller;

import com.automotive.costing.dashboard.dto.DashboardDtos.KpiResponse;
import com.automotive.costing.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/kpis")
    @PreAuthorize("@permissionAuth.has('DASHBOARD_VIEW')")
    @Operation(summary = "Operational KPI counts for the home dashboard")
    public ResponseEntity<KpiResponse> kpis() {
        return ResponseEntity.ok(dashboardService.kpis());
    }
}
