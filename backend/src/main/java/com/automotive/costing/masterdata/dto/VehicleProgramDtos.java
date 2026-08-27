package com.automotive.costing.masterdata.dto;

import java.math.BigDecimal;

public class VehicleProgramDtos {

    public record Response(
            String programId,
            String programCode,
            String modelName,
            String platformCode,
            String sourcingRegion,
            BigDecimal lifetimeVolume,
            BigDecimal targetCost,
            String targetCurrency,
            String status
    ) {
    }
}
