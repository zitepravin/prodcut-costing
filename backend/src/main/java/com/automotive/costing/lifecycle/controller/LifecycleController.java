package com.automotive.costing.lifecycle.controller;

import com.automotive.costing.lifecycle.dto.LifecycleDtos.CurveResponse;
import com.automotive.costing.lifecycle.dto.LifecycleDtos.LifecycleProjectionResponse;
import com.automotive.costing.lifecycle.dto.LifecycleDtos.UpsertCurveRequest;
import com.automotive.costing.lifecycle.service.LifecycleProjectionService;
import com.automotive.costing.lifecycle.service.VolumeCurveService;
import com.automotive.costing.platform.access.RequiresModule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/programs/{programId}")
@RequiredArgsConstructor
@Tag(name = "Lifecycle / Volume Curve")
@RequiresModule("PRODUCT_COSTING")
public class LifecycleController {

    private final VolumeCurveService volumeCurveService;
    private final LifecycleProjectionService lifecycleProjectionService;

    @GetMapping("/volume-curves")
    @Operation(summary = "List volume curves for a program")
    public ResponseEntity<List<CurveResponse>> listCurves(@PathVariable String programId) {
        return ResponseEntity.ok(volumeCurveService.list(programId));
    }

    @GetMapping("/volume-curves/{scenarioCode}")
    @Operation(summary = "Get volume curve by scenario code")
    public ResponseEntity<CurveResponse> getCurve(
            @PathVariable String programId,
            @PathVariable String scenarioCode
    ) {
        return ResponseEntity.ok(volumeCurveService.get(programId, scenarioCode));
    }

    @PutMapping("/volume-curves/{scenarioCode}")
    @Operation(summary = "Create or update volume curve years for a scenario")
    public ResponseEntity<CurveResponse> upsertCurve(
            @PathVariable String programId,
            @PathVariable String scenarioCode,
            @Valid @RequestBody UpsertCurveRequest request
    ) {
        return ResponseEntity.ok(volumeCurveService.upsert(programId, scenarioCode, request));
    }

    @PostMapping("/volume-curves/{scenarioCode}/activate")
    @Operation(summary = "Mark scenario curve ACTIVE")
    public ResponseEntity<CurveResponse> activate(
            @PathVariable String programId,
            @PathVariable String scenarioCode
    ) {
        return ResponseEntity.ok(volumeCurveService.activate(programId, scenarioCode));
    }

    @GetMapping("/lifecycle-projection")
    @Operation(summary = "Project unit and program cost by year for a volume-curve scenario")
    public ResponseEntity<LifecycleProjectionResponse> project(
            @PathVariable String programId,
            @RequestParam(defaultValue = "BASE") String scenarioCode
    ) {
        return ResponseEntity.ok(lifecycleProjectionService.project(programId, scenarioCode));
    }
}
