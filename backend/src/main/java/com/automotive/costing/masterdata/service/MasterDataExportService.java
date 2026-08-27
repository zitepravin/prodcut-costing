package com.automotive.costing.masterdata.service;

import com.automotive.costing.masterdata.dto.LaborMasterDtos;
import com.automotive.costing.masterdata.dto.MachineMasterDtos;
import com.automotive.costing.masterdata.dto.MaterialMasterDtos;
import com.automotive.costing.masterdata.entity.MasterDataStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * CSV export for master data entities (symmetric to bulk import templates).
 */
@Service
@RequiredArgsConstructor
public class MasterDataExportService {

    private final MaterialMasterService materialMasterService;
    private final MachineMasterService machineMasterService;
    private final LaborMasterService laborMasterService;

    @Transactional(readOnly = true)
    public String exportMaterialsCsv() {
        List<MaterialMasterDtos.Response> rows = materialMasterService
                .search(MasterDataStatus.ACTIVE, null, null, PageRequest.of(0, 10_000))
                .getContent();
        String header =
                "code,description,rate,uom,plantId,effectiveFrom,grade,specificationStandard,density,scrapRecoveryRate,materialForm";
        String body = rows.stream()
                .map(r -> String.join(",",
                        csv(r.code()),
                        csv(r.description()),
                        csv(r.rate()),
                        csv(r.uom()),
                        csv(r.plantId()),
                        csv(r.effectiveFrom()),
                        csv(r.grade()),
                        csv(r.specificationStandard()),
                        csv(r.density()),
                        csv(r.scrapRecoveryRate()),
                        csv(r.materialForm())))
                .collect(Collectors.joining("\n"));
        return header + "\n" + body + (body.isEmpty() ? "" : "\n");
    }

    @Transactional(readOnly = true)
    public String exportMachinesCsv() {
        List<MachineMasterDtos.Response> rows = machineMasterService
                .search(MasterDataStatus.ACTIVE, null, null, PageRequest.of(0, 10_000))
                .getContent();
        String header =
                "code,description,rate,uom,plantId,effectiveFrom,machineType,capacity,depreciationLifeYears,capitalCost,annualOperatingHours,powerCostPerHour,maintenanceCostPerHour,floorSpaceCostPerHour,shiftPattern";
        String body = rows.stream()
                .map(r -> String.join(",",
                        csv(r.code()),
                        csv(r.description()),
                        csv(r.rate()),
                        csv(r.uom()),
                        csv(r.plantId()),
                        csv(r.effectiveFrom()),
                        csv(r.machineType()),
                        csv(r.capacity()),
                        csv(r.depreciationLifeYears()),
                        csv(r.capitalCost()),
                        csv(r.annualOperatingHours()),
                        csv(r.powerCostPerHour()),
                        csv(r.maintenanceCostPerHour()),
                        csv(r.floorSpaceCostPerHour()),
                        csv(r.shiftPattern())))
                .collect(Collectors.joining("\n"));
        return header + "\n" + body + (body.isEmpty() ? "" : "\n");
    }

    @Transactional(readOnly = true)
    public String exportLaborCsv() {
        List<LaborMasterDtos.Response> rows = laborMasterService
                .search(MasterDataStatus.ACTIVE, null, null, PageRequest.of(0, 10_000))
                .getContent();
        String header =
                "code,description,rate,uom,plantId,effectiveFrom,operatorGrade,shiftType,countryRegion,statutoryBurdenPct";
        String body = rows.stream()
                .map(r -> String.join(",",
                        csv(r.code()),
                        csv(r.description()),
                        csv(r.rate()),
                        csv(r.uom()),
                        csv(r.plantId()),
                        csv(r.effectiveFrom()),
                        csv(r.operatorGrade()),
                        csv(r.shiftType()),
                        csv(r.countryRegion()),
                        csv(r.statutoryBurdenPct())))
                .collect(Collectors.joining("\n"));
        return header + "\n" + body + (body.isEmpty() ? "" : "\n");
    }

    private static String csv(Object value) {
        if (value == null) {
            return "";
        }
        String s = String.valueOf(value);
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
