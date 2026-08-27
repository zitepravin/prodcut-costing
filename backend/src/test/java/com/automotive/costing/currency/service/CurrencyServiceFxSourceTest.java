package com.automotive.costing.currency.service;

import com.automotive.costing.currency.dto.CurrencyDtos.ConvertRequest;
import com.automotive.costing.currency.dto.CurrencyDtos.ConvertResponse;
import com.automotive.costing.currency.entity.ExchangeRate;
import com.automotive.costing.currency.repository.ExchangeRateRepository;
import com.automotive.costing.masterdata.repository.CurrencyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrencyServiceFxSourceTest {

    @Mock CurrencyRepository currencyRepository;
    @Mock ExchangeRateRepository exchangeRateRepository;
    CurrencyService service;

    static final LocalDate HIST = LocalDate.of(2026, 4, 1);
    static final LocalDate LATER = LocalDate.of(2026, 8, 20);

    @BeforeEach
    void setUp() {
        service = new CurrencyService(currencyRepository, exchangeRateRepository);
    }

    private ExchangeRate rate(String from, String to, String val, LocalDate eff, String source) {
        ExchangeRate er = new ExchangeRate();
        er.setRateId("r-" + from + to + eff);
        er.setFromCurrency(from);
        er.setToCurrency(to);
        er.setRate(new BigDecimal(val));
        er.setEffectiveDate(eff);
        er.setSource(source);
        return er;
    }

    @Test void historicalRate_usesAprilSource_notAugust() {
        when(exchangeRateRepository.findEffectiveRate("USD", "INR", HIST))
                .thenReturn(Optional.of(rate("USD", "INR", "84.50", HIST, "CENTRAL_BANK")));

        ConvertResponse r = service.convert(new ConvertRequest(new BigDecimal("100"), "USD", "INR", HIST));
        assertThat(r.convertedAmount()).isEqualByComparingTo("8450.000000");
        assertThat(r.rateUsed()).isEqualByComparingTo("84.50000000");
        assertThat(r.rateEffectiveDate()).isEqualTo(HIST);
        assertThat(r.rateSource()).isEqualTo("CENTRAL_BANK");
        assertThat(r.conversionPath()).isEqualTo("DIRECT");
        verify(exchangeRateRepository).findEffectiveRate("USD", "INR", HIST);
        verify(exchangeRateRepository, never()).findEffectiveRate("USD", "INR", LATER);
    }

    @Test void afterMasterChange_newAsOf_selectsNewRate() {
        when(exchangeRateRepository.findEffectiveRate("USD", "INR", LATER))
                .thenReturn(Optional.of(rate("USD", "INR", "86.00", LATER, "TREASURY")));
        ConvertResponse r = service.convert(new ConvertRequest(new BigDecimal("100"), "USD", "INR", LATER));
        assertThat(r.convertedAmount()).isEqualByComparingTo("8600.000000");
        assertThat(r.rateSource()).isEqualTo("TREASURY");
        assertThat(r.rateEffectiveDate()).isEqualTo(LATER);
    }

    @Test void frozenHistoricalResult_independentOfLaterMaster() {
        // Simulate: first conversion at HIST with 84.50
        when(exchangeRateRepository.findEffectiveRate("USD", "INR", HIST))
                .thenReturn(Optional.of(rate("USD", "INR", "84.50", HIST, "CENTRAL_BANK")));
        ConvertResponse frozen = service.convert(new ConvertRequest(new BigDecimal("100"), "USD", "INR", HIST));
        assertThat(frozen.convertedAmount()).isEqualByComparingTo("8450.000000");
        assertThat(frozen.rateSource()).isEqualTo("CENTRAL_BANK");

        // Master now returns 86 for LATER — frozen asOf still returns 84.50
        when(exchangeRateRepository.findEffectiveRate("USD", "INR", HIST))
                .thenReturn(Optional.of(rate("USD", "INR", "84.50", HIST, "CENTRAL_BANK")));
        ConvertResponse reRead = service.convert(new ConvertRequest(new BigDecimal("100"), "USD", "INR", HIST));
        assertThat(reRead.convertedAmount()).isEqualByComparingTo("8450.000000");
        assertThat(reRead.rateSource()).isEqualTo("CENTRAL_BANK");

        // New costing at LATER uses 86
        when(exchangeRateRepository.findEffectiveRate("USD", "INR", LATER))
                .thenReturn(Optional.of(rate("USD", "INR", "86.00", LATER, "TREASURY")));
        ConvertResponse neu = service.convert(new ConvertRequest(new BigDecimal("100"), "USD", "INR", LATER));
        assertThat(neu.convertedAmount()).isEqualByComparingTo("8600.000000");
        assertThat(neu.rateSource()).isEqualTo("TREASURY");
    }

    @Test void identity_rateSourceNull() {
        ConvertResponse r = service.convert(new ConvertRequest(new BigDecimal("10"), "USD", "USD", HIST));
        assertThat(r.conversionPath()).isEqualTo("IDENTITY");
        assertThat(r.rateSource()).isNull();
        verifyNoInteractions(exchangeRateRepository);
    }
}
