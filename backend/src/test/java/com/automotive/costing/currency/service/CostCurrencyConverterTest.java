package com.automotive.costing.currency.service;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.currency.dto.CurrencyDtos.ConvertRequest;
import com.automotive.costing.currency.dto.CurrencyDtos.ConvertResponse;
import com.automotive.costing.currency.service.CostCurrencyConverter.FxConversionResult;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CostCurrencyConverterTest {

    @Mock private CurrencyService currencyService;
    private CostCurrencyConverter converter;
    private static final LocalDate HISTORICAL = LocalDate.of(2026, 4, 1);

    @BeforeEach
    void setUp() {
        converter = new CostCurrencyConverter(currencyService);
        ReflectionTestUtils.setField(converter, "baseCurrency", "USD");
    }

    @Test void sameCurrency_noFx() {
        assertThat(converter.toProgramCurrency(new BigDecimal("10"), "USD", HISTORICAL))
                .isEqualByComparingTo("10");
        verifyNoInteractions(currencyService);
    }

    @Test void historical_preservesRateSource() {
        when(currencyService.convert(any(ConvertRequest.class))).thenReturn(
                new ConvertResponse(new BigDecimal("100"), "USD", new BigDecimal("8450"), "INR",
                        new BigDecimal("84.50"), HISTORICAL, "DIRECT", "CENTRAL_BANK"));
        FxConversionResult fx = converter.convertToProgramCurrency(new BigDecimal("100"), "INR", HISTORICAL);
        assertThat(fx.convertedAmount()).isEqualByComparingTo("8450");
        assertThat(fx.rateSource()).isEqualTo("CENTRAL_BANK");
        assertThat(fx.asOfDate()).isEqualTo(HISTORICAL);
        ArgumentCaptor<ConvertRequest> c = ArgumentCaptor.forClass(ConvertRequest.class);
        verify(currencyService).convert(c.capture());
        assertThat(c.getValue().asOfDate()).isEqualTo(HISTORICAL);
    }

    @Test void crossCurrencyWithoutAsOf_throws() {
        assertThatThrownBy(() -> converter.toProgramCurrency(new BigDecimal("100"), "INR"))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test void identity_rateSourceNull() {
        FxConversionResult fx = converter.convertToProgramCurrency(new BigDecimal("50"), "USD", HISTORICAL);
        assertThat(fx.rateSource()).isNull();
        assertThat(fx.isIdentity()).isTrue();
    }
}
