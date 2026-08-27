package com.automotive.costing.masterdata.controller;

import com.automotive.costing.masterdata.dto.VehicleProgramDtos.Response;
import com.automotive.costing.masterdata.service.VehicleProgramService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import com.automotive.costing.platform.access.RequiresModule;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * M01 vehicle program catalog — supports BOM tree program selection and should-cost region fallback.
 */
@RestController
@RequestMapping("/api/v1/master/programs")
@RequiredArgsConstructor
@Tag(name = "Master Data - Vehicle Programs")
@RequiresModule("PRODUCT_COSTING")
public class VehicleProgramController {

    private final VehicleProgramService service;

    @GetMapping
    @Operation(summary = "List all vehicle programs")
    public ResponseEntity<List<Response>> list() {
        return ResponseEntity.ok(service.listAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a vehicle program by id")
    public ResponseEntity<Response> getById(@PathVariable String id) {
        return ResponseEntity.ok(service.getById(id));
    }
}
