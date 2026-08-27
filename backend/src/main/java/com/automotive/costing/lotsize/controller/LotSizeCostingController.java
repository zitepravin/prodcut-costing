package com.automotive.costing.lotsize.controller;

import com.automotive.costing.lotsize.dto.LotSizeDtos.CurveRequest;
import com.automotive.costing.lotsize.dto.LotSizeDtos.CurveResponse;
import com.automotive.costing.lotsize.service.LotSizeCostingService;
import com.automotive.costing.platform.access.RequiresModule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/lot-size-costing")
@RequiredArgsConstructor
@Tag(name = "Lot-Size Costing (B2)")
@RequiresModule("PRODUCT_COSTING")
public class LotSizeCostingController {

    private final LotSizeCostingService service;

    @PostMapping("/curve")
    @Operation(summary = "Unit cost curve for standard or custom lot sizes")
    public ResponseEntity<CurveResponse> curve(@Valid @RequestBody CurveRequest request) {
        return ResponseEntity.ok(service.curve(request));
    }
}
