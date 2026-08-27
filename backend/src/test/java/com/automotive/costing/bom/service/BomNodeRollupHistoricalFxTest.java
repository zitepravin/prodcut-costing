package com.automotive.costing.bom.service;

import com.automotive.costing.currency.dto.CurrencyDtos.ConvertRequest;
import com.automotive.costing.currency.dto.CurrencyDtos.ConvertResponse;
import com.automotive.costing.currency.service.CostCurrencyConverter;
import com.automotive.costing.currency.service.CurrencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Regression: BOM roll-up valuation date must be the explicit costing date,
 * not system today, when provided by the caller.
 */
@ExtendWith(MockitoExtension.class)
class BomNodeRollupHistoricalFxTest {

    @Mock CurrencyService currencyService;
    CostCurrencyConverter converter;
    static final LocalDate HISTORICAL = LocalDate.of(2026, 4, 1);
    static final LocalDate TODAY = LocalDate.of(2026, 8, 20);

    @BeforeEach
    void setUp() {
        converter = new CostCurrencyConverter(currencyService);
        ReflectionTestUtils.setField(converter, "baseCurrency", "USD");
    }

    @Test
    void convertUsesHistoricalAsOf_notToday() {
        when(currencyService.convert(any(ConvertRequest.class))).thenReturn(
                new ConvertResponse(new BigDecimal("100"), "USD", new BigDecimal("8450"), "INR",
                        new BigDecimal("84.50"), HISTORICAL, "DIRECT", "CENTRAL_BANK"));

        BigDecimal result = converter.toProgramCurrency(new BigDecimal("100"), "INR", HISTORICAL);
        assertThat(result).isEqualByComparingTo("8450");

        ArgumentCaptor<ConvertRequest> cap = ArgumentCaptor.forClass(ConvertRequest.class);
        verify(currencyService).convert(cap.capture());
        assertThat(cap.getValue().asOfDate()).isEqualTo(HISTORICAL);
        assertThat(cap.getValue().asOfDate()).isNotEqualTo(TODAY);
    }
}
