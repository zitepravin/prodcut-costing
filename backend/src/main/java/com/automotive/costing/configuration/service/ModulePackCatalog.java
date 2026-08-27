package com.automotive.costing.configuration.service;

import com.automotive.costing.configuration.dto.ConfigurationDtos.ModulePackInfo;

import java.util.List;

/** P3-07d — commercial module pack catalog (apply via existing entitlements). */
public final class ModulePackCatalog {
    private ModulePackCatalog() {}

    public static List<ModulePackInfo> all() {
        return List.of(
                new ModulePackInfo(
                        "COSTING_ONLY",
                        "Product Costing only",
                        List.of("PRODUCT_COSTING"),
                        "Should-cost, BOM, targets, variance"),
                new ModulePackInfo(
                        "COSTING_PLUS_VS",
                        "Costing + Vendor Selection",
                        List.of("PRODUCT_COSTING", "VENDOR_SELECTION"),
                        "Adds scorecard, TCO, questionnaires"),
                new ModulePackInfo(
                        "FULL_SOURCING",
                        "Full sourcing",
                        List.of("PRODUCT_COSTING", "VENDOR_SELECTION", "PURCHASE"),
                        "Adds purchase request, tracking, delivery performance")
        );
    }
}
