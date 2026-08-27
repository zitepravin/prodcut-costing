package com.automotive.costing.masterdata.controller;

import com.automotive.costing.masterdata.service.PlantMasterService;
import com.automotive.costing.masterdata.service.PlantMasterService.CreateRequest;
import com.automotive.costing.masterdata.service.PlantMasterService.Response;
import com.automotive.costing.platform.access.RequiresModule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Tenant-owned plant/site catalog HTTP surface.
 * Create enforces commercial plant_site_limit via PlantMasterService.
 */
@RestController
@RequestMapping("/api/v1/master/plants")
@RequiredArgsConstructor
@Tag(name = "Master Data - Plants")
@RequiresModule("PRODUCT_COSTING")
public class PlantMasterController {

    private final PlantMasterService service;

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','COSTING_ENGINEER','COSTING_MANAGER','FINANCE_CONTROLLER',"
            + "'MASTER_DATA_ADMIN','CUSTOMER_ADMIN','SYSTEM_ADMINISTRATOR','ADMIN','PLATFORM_OWNER')")
    @Operation(summary = "List plants for current customer")
    public List<Response> list() {
        return service.list();
    }

    @GetMapping("/{plantId}")
    @PreAuthorize("hasAnyRole('VIEWER','COSTING_ENGINEER','COSTING_MANAGER','FINANCE_CONTROLLER',"
            + "'MASTER_DATA_ADMIN','CUSTOMER_ADMIN','SYSTEM_ADMINISTRATOR','ADMIN','PLATFORM_OWNER')")
    @Operation(summary = "Get plant by ID")
    public Response get(@PathVariable String plantId) {
        return service.get(plantId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MASTER_DATA_ADMIN','COSTING_MANAGER','CUSTOMER_ADMIN',"
            + "'SYSTEM_ADMINISTRATOR','ADMIN','PLATFORM_OWNER')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create plant/site (subject to commercial plant_site_limit)")
    public ResponseEntity<Response> create(@RequestBody CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }
}
