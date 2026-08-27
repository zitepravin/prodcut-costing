package com.automotive.costing.currency.controller;

import com.automotive.costing.platform.access.RequiresModule;

import com.automotive.costing.currency.dto.CurrencyDtos.*;
import com.automotive.costing.currency.service.CurrencyService;
import com.automotive.costing.currency.service.ExchangeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * M12 Currency Management — list currencies, publish FX rates, convert amounts.
 * Roles: MASTER_DATA_ADMIN / COSTING_ENGINEER for publish; any authenticated for read/convert.
 */
@RestController
@RequestMapping("/api/v1/currency")
@RequiredArgsConstructor
@Tag(name = "Currency Management")
@RequiresModule("PRODUCT_COSTING")
public class CurrencyController {

    private final CurrencyService currencyService;
    private final ExchangeRateService exchangeRateService;

    @GetMapping
    @Operation(summary = "List all supported currencies")
    public ResponseEntity<List<CurrencyResponse>> listCurrencies() {
        return ResponseEntity.ok(currencyService.listCurrencies());
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get a single currency by ISO code")
    public ResponseEntity<CurrencyResponse> getCurrency(@PathVariable String code) {
        return ResponseEntity.ok(currencyService.getCurrency(code));
    }

    @PostMapping("/rates")
    @PreAuthorize("hasAnyRole('MASTER_DATA_ADMIN', 'COSTING_ENGINEER', 'ADMIN')")
    @Operation(summary = "Publish a new exchange rate (append-only; CUR publish)")
    public ResponseEntity<ExchangeRateResponse> publishRate(@Valid @RequestBody PublishRateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(exchangeRateService.publish(request));
    }

    @GetMapping("/rates/{from}/{to}")
    @Operation(summary = "Rate history for a currency pair (newest first)")
    public ResponseEntity<RateHistoryResponse> rateHistory(
            @PathVariable String from,
            @PathVariable String to) {
        return ResponseEntity.ok(exchangeRateService.history(from, to));
    }

    @GetMapping("/rates")
    @Operation(summary = "Recent rates since a date (default 90 days)")
    public ResponseEntity<List<ExchangeRateResponse>> recentRates(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate since) {
        LocalDate from = since != null ? since : LocalDate.now().minusDays(90);
        return ResponseEntity.ok(exchangeRateService.recentRates(from));
    }

    @PostMapping("/convert")
    @Operation(summary = "Convert an amount using effective rate (DIRECT / INVERSE / VIA_USD)")
    public ResponseEntity<ConvertResponse> convert(@Valid @RequestBody ConvertRequest request) {
        return ResponseEntity.ok(currencyService.convert(request));
    }
}
