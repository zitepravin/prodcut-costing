package com.automotive.costing.vendorselection.engine;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic TCO engine (Vendor Selection B3).
 * Missing components are marked DATA_NOT_AVAILABLE — never assumed zero unless explicitly provided.
 * All money uses {@link BigDecimal}.
 */
@Component
public class TcoCalculationEngine {

    public static final String DATA_NOT_AVAILABLE = "DATA_NOT_AVAILABLE";
    private static final int SCALE = 6;

    public record TcoInput(
            BigDecimal purchasePrice,
            BigDecimal freight,
            BigDecimal logistics,
            BigDecimal packaging,
            BigDecimal inventoryCost,
            BigDecimal qualityRejectionCost,
            BigDecimal reworkCost,
            BigDecimal warrantyCost,
            BigDecimal serviceCost,
            BigDecimal toolingCost,
            BigDecimal otherCost,
            BigDecimal annualVolume
    ) {}

    public record ComponentLine(
            String code,
            String status,
            BigDecimal amount
    ) {}

    public record TcoOutput(
            BigDecimal tcoTotal,
            boolean dataComplete,
            List<String> missingComponents,
            List<ComponentLine> components,
            /** Unit TCO when volume present: total / volume; null if volume missing or zero. */
            BigDecimal unitTco
    ) {}

    public TcoOutput calculate(TcoInput input) {
        Map<String, BigDecimal> provided = new LinkedHashMap<>();
        putIfPresent(provided, "PURCHASE_PRICE", input.purchasePrice());
        putIfPresent(provided, "FREIGHT", input.freight());
        putIfPresent(provided, "LOGISTICS", input.logistics());
        putIfPresent(provided, "PACKAGING", input.packaging());
        putIfPresent(provided, "INVENTORY_COST", input.inventoryCost());
        putIfPresent(provided, "QUALITY_REJECTION_COST", input.qualityRejectionCost());
        putIfPresent(provided, "REWORK_COST", input.reworkCost());
        putIfPresent(provided, "WARRANTY_COST", input.warrantyCost());
        putIfPresent(provided, "SERVICE_COST", input.serviceCost());
        putIfPresent(provided, "TOOLING_COST", input.toolingCost());
        putIfPresent(provided, "OTHER_COST", input.otherCost());

        List<String> allCodes = List.of(
                "PURCHASE_PRICE", "FREIGHT", "LOGISTICS", "PACKAGING", "INVENTORY_COST",
                "QUALITY_REJECTION_COST", "REWORK_COST", "WARRANTY_COST", "SERVICE_COST",
                "TOOLING_COST", "OTHER_COST");

        List<ComponentLine> lines = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);

        for (String code : allCodes) {
            if (provided.containsKey(code)) {
                BigDecimal amt = provided.get(code).setScale(SCALE, RoundingMode.HALF_UP);
                lines.add(new ComponentLine(code, "PROVIDED", amt));
                sum = sum.add(amt);
            } else {
                missing.add(code);
                lines.add(new ComponentLine(code, DATA_NOT_AVAILABLE, null));
            }
        }

        boolean complete = missing.isEmpty() && provided.containsKey("PURCHASE_PRICE");
        // TCO total is sum of provided components only; incomplete flag set when any missing
        BigDecimal unit = null;
        if (input.annualVolume() != null && input.annualVolume().compareTo(BigDecimal.ZERO) > 0
                && provided.containsKey("PURCHASE_PRICE")) {
            unit = sum.divide(input.annualVolume(), SCALE, RoundingMode.HALF_UP);
        }

        return new TcoOutput(sum, complete && missing.isEmpty(), missing, lines, unit);
    }

    private static void putIfPresent(Map<String, BigDecimal> map, String code, BigDecimal value) {
        if (value != null) {
            map.put(code, value);
        }
    }
}
