package com.automotive.costing.currency.repository;

import com.automotive.costing.currency.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, String> {

    Optional<ExchangeRate> findFirstByFromCurrencyAndToCurrencyAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
            String fromCurrency, String toCurrency, LocalDate asOf);

    default Optional<ExchangeRate> findEffectiveRate(String fromCurrency, String toCurrency, LocalDate asOf) {
        return findFirstByFromCurrencyAndToCurrencyAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                fromCurrency, toCurrency, asOf);
    }

    List<ExchangeRate> findByFromCurrencyAndToCurrencyOrderByEffectiveDateDesc(
            String fromCurrency, String toCurrency);

    List<ExchangeRate> findByEffectiveDateGreaterThanEqualOrderByEffectiveDateDesc(LocalDate fromDate);
}
