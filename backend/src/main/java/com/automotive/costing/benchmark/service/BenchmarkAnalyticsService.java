package com.automotive.costing.benchmark.service;

import com.automotive.costing.benchmark.entity.BenchmarkCategory;
import com.automotive.costing.benchmark.entity.BenchmarkRate;
import com.automotive.costing.benchmark.repository.BenchmarkRateRepository;
import com.automotive.costing.rfq.entity.QuoteStatus;
import com.automotive.costing.rfq.entity.SupplierQuote;
import com.automotive.costing.rfq.repository.SupplierQuoteRepository;
import com.automotive.costing.shouldcost.entity.ShouldCostBuildUp;
import com.automotive.costing.shouldcost.repository.ShouldCostBuildUpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * Market-gap: should-cost vs quote vs benchmark analytics. Missing = DATA_NOT_AVAILABLE.
 */
@Service
@RequiredArgsConstructor
public class BenchmarkAnalyticsService {

    private static final int SCALE = 6;

    private final ShouldCostBuildUpRepository shouldCostBuildUpRepository;
    private final SupplierQuoteRepository quoteRepository;
    private final BenchmarkRateRepository rateRepository;

    public record AnalyticsResponse(
            String bomNodeId,
            BigDecimal shouldCost,
            String shouldStatus,
            BigDecimal bestQuote,
            String quoteStatus,
            BigDecimal benchmarkRate,
            String benchmarkCategory,
            String benchmarkStatus,
            BigDecimal shouldVsQuote,
            BigDecimal shouldVsBenchmark,
            BigDecimal quoteVsBenchmark,
            String summary
    ) {}

    @Transactional(readOnly = true)
    public AnalyticsResponse analyze(String bomNodeId, String benchmarkCategory) {
        Optional<ShouldCostBuildUp> sc = shouldCostBuildUpRepository
                .findByBomNodeIdOrderByCalculatedAtDesc(bomNodeId).stream().findFirst();
        BigDecimal should = sc.map(ShouldCostBuildUp::getTotalCost).orElse(null);

        List<SupplierQuote> quotes = quoteRepository
                .findByBomNodeIdAndStatusOrderByUpdatedAtDesc(bomNodeId, QuoteStatus.ACCEPTED);
        if (quotes.isEmpty()) {
            quotes = quoteRepository.findByBomNodeIdOrderByUpdatedAtDesc(bomNodeId);
        }
        BigDecimal bestQuote = quotes.stream()
                .map(SupplierQuote::getUnitPrice)
                .filter(p -> p != null)
                .min(BigDecimal::compareTo)
                .orElse(null);

        BigDecimal bench = null;
        String benchCat = benchmarkCategory;
        String benchStatus = "DATA_NOT_AVAILABLE";
        if (benchmarkCategory != null && !benchmarkCategory.isBlank()) {
            try {
                BenchmarkCategory cat = BenchmarkCategory.valueOf(benchmarkCategory.trim().toUpperCase());
                Optional<BenchmarkRate> rate = rateRepository.findAll().stream()
                        .filter(r -> r.getCategory() == cat && r.getRateValue() != null)
                        .findFirst();
                if (rate.isPresent()) {
                    bench = rate.get().getRateValue();
                    benchCat = cat.name();
                    benchStatus = "OK";
                }
            } catch (IllegalArgumentException ignored) {
                benchStatus = "DATA_NOT_AVAILABLE";
            }
        }

        return new AnalyticsResponse(
                bomNodeId,
                should, should != null ? "OK" : "DATA_NOT_AVAILABLE",
                bestQuote, bestQuote != null ? "OK" : "DATA_NOT_AVAILABLE",
                bench, benchCat, benchStatus,
                delta(should, bestQuote),
                delta(should, bench),
                delta(bestQuote, bench),
                "Should vs quote vs benchmark — missing buckets stay DATA_NOT_AVAILABLE."
        );
    }

    private static BigDecimal delta(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) return null;
        return a.subtract(b).setScale(SCALE, RoundingMode.HALF_UP);
    }
}
