package com.automotive.costing.process.engine;

import com.automotive.costing.common.costing.CostComponentResult;
import com.automotive.costing.process.engine.ConsumableCostEngine.ConsumableLine;
import com.automotive.costing.process.engine.ConsumableCostEngine.ConsumableType;
import com.automotive.costing.process.engine.InspectionCostEngine.InspectionActivity;
import com.automotive.costing.process.engine.ProcessCostEngine.OperationComponentResult;
import com.automotive.costing.process.engine.ProcessCostEngine.OperationCostInput;
import com.automotive.costing.process.engine.ProcessCostEngine.SecondaryCostInput;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProcessCostComponentReconciliationTest {

    @Test
    void operationTotal_equalsSumOfComponents() {
        OperationCostInput op = new OperationCostInput(
                "OP-10",
                new BigDecimal("3600"), // 1 hour cycle
                new BigDecimal("100"),  // machine $/h
                new BigDecimal("50"),   // labor $/h
                BigDecimal.ONE,
                new BigDecimal("1800"), // 0.5h setup
                new BigDecimal("10"),   // batch 10
                null);

        SecondaryCostInput secondary = new SecondaryCostInput(
                new BigDecimal("5"),           // kW
                new BigDecimal("2.00"),         // electricity $/kWh
                "USD", "TARIFF", LocalDate.of(2026, 4, 1),
                List.of(new ConsumableLine(ConsumableType.WIRE, new BigDecimal("0.1"), "kg",
                        new BigDecimal("20"), "USD", "VENDOR", LocalDate.of(2026, 4, 1))),
                List.of(new InspectionActivity("insp-1", new BigDecimal("0.05"),
                        new BigDecimal("40"), "USD", "LABOR", LocalDate.of(2026, 4, 1), null)),
                new BigDecimal("3.00"), // external
                LocalDate.of(2026, 4, 1),
                "bom-1"
        );

        OperationComponentResult r = ProcessCostEngine.calculateOperationWithComponents(op, secondary);

        // Expected:
        // machine = 1h * 100 = 100
        // labor = 1h * 50 * 1 = 50
        // setup = 0.5h * (100+50) / 10 = 7.5
        // power = 5 kW * 1h * 2 = 10
        // consumable = 0.1 * 20 = 2
        // inspection = 0.05 * 40 = 2
        // external = 3
        // total = 100+50+7.5+10+2+2+3 = 174.5

        assertEquals(0, new BigDecimal("7.500000").compareTo(r.setupCost()));
        assertEquals(0, new BigDecimal("100.000000").compareTo(r.machineCost()));
        assertEquals(0, new BigDecimal("50.000000").compareTo(r.directLaborCost()));
        assertEquals(0, new BigDecimal("10.000000").compareTo(r.powerCost()));
        assertEquals(0, new BigDecimal("2.000000").compareTo(r.consumableCost()));
        assertEquals(0, new BigDecimal("2.000000").compareTo(r.inspectionCost()));
        assertEquals(0, new BigDecimal("3.000000").compareTo(r.externalProcessCost()));
        assertEquals(0, new BigDecimal("174.500000").compareTo(r.totalOperationCost()));

        BigDecimal sumComponents = r.components().stream()
                .map(CostComponentResult::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, r.totalOperationCost().compareTo(sumComponents.setScale(6, java.math.RoundingMode.HALF_UP)));
    }
}
