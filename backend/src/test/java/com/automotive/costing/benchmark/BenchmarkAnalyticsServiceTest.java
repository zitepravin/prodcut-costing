package com.automotive.costing.benchmark;

import com.automotive.costing.benchmark.repository.BenchmarkRateRepository;
import com.automotive.costing.benchmark.service.BenchmarkAnalyticsService;
import com.automotive.costing.rfq.repository.SupplierQuoteRepository;
import com.automotive.costing.shouldcost.entity.ShouldCostBuildUp;
import com.automotive.costing.shouldcost.repository.ShouldCostBuildUpRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BenchmarkAnalyticsServiceTest {

    @Mock ShouldCostBuildUpRepository shouldCostBuildUpRepository;
    @Mock SupplierQuoteRepository quoteRepository;
    @Mock BenchmarkRateRepository rateRepository;
    @InjectMocks BenchmarkAnalyticsService service;

    @Test
    void missingQuoteAndBenchmark_markedUnavailable() {
        ShouldCostBuildUp sc = new ShouldCostBuildUp();
        sc.setTotalCost(new BigDecimal("100"));
        when(shouldCostBuildUpRepository.findByBomNodeIdOrderByCalculatedAtDesc("n1"))
                .thenReturn(List.of(sc));
        when(quoteRepository.findByBomNodeIdAndStatusOrderByUpdatedAtDesc(any(), any()))
                .thenReturn(List.of());
        when(quoteRepository.findByBomNodeIdOrderByUpdatedAtDesc("n1")).thenReturn(List.of());
        when(rateRepository.findAll()).thenReturn(List.of());

        var r = service.analyze("n1", "MATERIAL");
        assertEquals("OK", r.shouldStatus());
        assertEquals("DATA_NOT_AVAILABLE", r.quoteStatus());
        assertEquals("DATA_NOT_AVAILABLE", r.benchmarkStatus());
        assertNull(r.shouldVsQuote());
    }
}
