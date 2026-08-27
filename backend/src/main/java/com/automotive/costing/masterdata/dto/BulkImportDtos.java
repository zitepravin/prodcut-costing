package com.automotive.costing.masterdata.dto;

import java.util.List;

/** MDM-04: bulk import result reporting, mirroring the dry-run/error-report pattern used in M20. */
public class BulkImportDtos {

    public record RowError(int rowNumber, String reason) {
    }

    public record ImportResult(
            int totalRows,
            int succeeded,
            int failed,
            List<RowError> errors
    ) {
    }
}
