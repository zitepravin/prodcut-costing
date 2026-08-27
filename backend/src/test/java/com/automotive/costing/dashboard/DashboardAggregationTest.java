package com.automotive.costing.dashboard;

import com.automotive.costing.process.repository.ProcessOperationRepository;
import com.automotive.costing.purchase.repository.PurchaseOrderLineRepository;
import com.automotive.costing.rfq.repository.RfqEventRepository;
import com.automotive.costing.tooling.repository.ToolingRecordRepository;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardAggregationTest {

    @Test
    void rfqRepositoryExposesOpenAndClosingSoonCounts() {
        assertThat(methodNames(RfqEventRepository.class))
                .contains("countByCustomerIdAndStatus", "countByCustomerIdAndStatusAndDueDateBetween", "countOpenWithNoQuotes");
    }

    @Test
    void purchaseSpendQueryExists() {
        assertThat(methodNames(PurchaseOrderLineRepository.class)).contains("sumLineValueByCustomerIdAndStatusIn");
    }

    @Test
    void routingAndToolingAggregatesExist() {
        assertThat(methodNames(ProcessOperationRepository.class))
                .contains("countMissingMachineOrLaborByCustomerId", "countNonPositiveCycleTimeByCustomerId");
        assertThat(methodNames(ToolingRecordRepository.class))
                .contains("sumToolCostByCustomerIdAndAssetStatus", "countByCustomerIdAndAssetStatus");
    }

    private static java.util.Set<String> methodNames(Class<?> type) {
        return Arrays.stream(type.getMethods()).map(Method::getName).collect(java.util.stream.Collectors.toSet());
    }
}
