package com.automotive.costing.erp.sap.service;

import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.erp.sap.dto.SapCostDtos.CompareRequest;
import com.automotive.costing.erp.sap.dto.SapCostDtos.CompareResponse;
import com.automotive.costing.erp.sap.dto.SapCostDtos.ComponentIn;
import com.automotive.costing.erp.sap.dto.SapCostDtos.ComponentOut;
import com.automotive.costing.erp.sap.dto.SapCostDtos.ComponentVariance;
import com.automotive.costing.erp.sap.dto.SapCostDtos.LandRequest;
import com.automotive.costing.erp.sap.dto.SapCostDtos.MappingResponse;
import com.automotive.costing.erp.sap.dto.SapCostDtos.MappingUpsert;
import com.automotive.costing.erp.sap.dto.SapCostDtos.StagingResponse;
import com.automotive.costing.erp.sap.engine.SapShouldCostCompareEngine;
import com.automotive.costing.erp.sap.engine.SapShouldCostCompareEngine.CompareResult;
import com.automotive.costing.erp.sap.entity.SapComponentMapping;
import com.automotive.costing.erp.sap.entity.SapCostEstimateComponentStaging;
import com.automotive.costing.erp.sap.entity.SapCostEstimateStaging;
import com.automotive.costing.erp.sap.repository.SapComponentMappingRepository;
import com.automotive.costing.erp.sap.repository.SapCostEstimateComponentStagingRepository;
import com.automotive.costing.erp.sap.repository.SapCostEstimateStagingRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.shouldcost.entity.ShouldCostBuildUp;
import com.automotive.costing.shouldcost.repository.ShouldCostBuildUpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SapCostEstimateService {

    private final SapCostEstimateStagingRepository stagingRepository;
    private final SapCostEstimateComponentStagingRepository componentRepository;
    private final SapComponentMappingRepository mappingRepository;
    private final ShouldCostBuildUpRepository buildUpRepository;
    private final TenantAccessGuard tenantAccessGuard;

    @Transactional
    public StagingResponse land(LandRequest request) {
        if (request.sapMaterial() == null || request.sapMaterial().isBlank()) {
            throw new IllegalArgumentException("sapMaterial is required");
        }
        if (request.sapPlant() == null || request.sapPlant().isBlank()) {
            throw new IllegalArgumentException("sapPlant is required");
        }

        Instant now = Instant.now();
        SapCostEstimateStaging s = new SapCostEstimateStaging();
        s.setStagingId(UUID.randomUUID().toString());
        s.setSourceSystem(request.sourceSystem() != null ? request.sourceSystem() : "SAP");
        s.setSapMaterial(request.sapMaterial().trim());
        s.setSapPlant(request.sapPlant().trim());
        s.setCostingVariant(request.costingVariant());
        s.setCostingVersion(request.costingVersion());
        s.setEstimateDate(request.estimateDate());
        s.setCurrencyCode(request.currencyCode());
        s.setTotalCost(request.totalCost());
        s.setStatus("LANDED");
        s.setRawPayload(request.rawPayload());
        s.setReceivedAt(now);
        s.setCreatedAt(now);
        stagingRepository.save(s);

        List<ComponentIn> comps = request.components() != null ? request.components() : List.of();
        int seq = 1;
        for (ComponentIn c : comps) {
            if (c.sapComponentCode() == null || c.amount() == null) {
                throw new IllegalArgumentException("Each component requires sapComponentCode and amount");
            }
            SapCostEstimateComponentStaging row = new SapCostEstimateComponentStaging();
            row.setStagingId(s.getStagingId());
            row.setComponentSeq(seq++);
            row.setSapComponentCode(c.sapComponentCode().trim());
            row.setSapComponentName(c.sapComponentName());
            row.setAmount(c.amount());
            mappingRepository.findBySapComponentCodeAndActiveFlagTrue(c.sapComponentCode().trim())
                    .ifPresent(m -> row.setMappedComponentCode(m.getOurComponentCode()));
            componentRepository.save(row);
        }

        if (!comps.isEmpty()) {
            s.setStatus("MAPPED");
            s.setUpdatedAt(now);
            stagingRepository.save(s);
        }

        return toStagingResponse(s, List.of("Landed from " + s.getSourceSystem()));
    }

    @Transactional(readOnly = true)
    public List<StagingResponse> list() {
        return stagingRepository.findAllByOrderByReceivedAtDesc().stream()
                .map(s -> toStagingResponse(s, List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public StagingResponse get(String stagingId) {
        return toStagingResponse(requireStaging(stagingId), List.of());
    }

    @Transactional
    public StagingResponse remap(String stagingId) {
        SapCostEstimateStaging s = requireStaging(stagingId);
        List<SapCostEstimateComponentStaging> comps =
                componentRepository.findByStagingIdOrderByComponentSeqAsc(stagingId);
        for (SapCostEstimateComponentStaging c : comps) {
            mappingRepository.findBySapComponentCodeAndActiveFlagTrue(c.getSapComponentCode())
                    .ifPresentOrElse(
                            m -> c.setMappedComponentCode(m.getOurComponentCode()),
                            () -> c.setMappedComponentCode(null)
                    );
            componentRepository.save(c);
        }
        s.setStatus("MAPPED");
        s.setUpdatedAt(Instant.now());
        stagingRepository.save(s);
        return toStagingResponse(s, List.of("Component mapping refreshed"));
    }

    @Transactional
    public CompareResponse compare(String stagingId, CompareRequest request) {
        SapCostEstimateStaging s = requireStaging(stagingId);
        if (request == null || request.shouldCostBuildUpId() == null || request.shouldCostBuildUpId().isBlank()) {
            throw new IllegalArgumentException("shouldCostBuildUpId is required");
        }
        String customerId = tenantAccessGuard.requireCustomerId();
        ShouldCostBuildUp bu = buildUpRepository
                .findByCustomerIdAndBuildUpId(customerId, request.shouldCostBuildUpId().trim())
                .orElseThrow(() -> ResourceNotFoundException.of("ShouldCostBuildUp", request.shouldCostBuildUpId()));

        List<SapCostEstimateComponentStaging> comps =
                componentRepository.findByStagingIdOrderByComponentSeqAsc(stagingId);

        Map<String, BigDecimal> sapByOur = new LinkedHashMap<>();
        for (SapCostEstimateComponentStaging c : comps) {
            String code = c.getMappedComponentCode();
            if (code == null || code.isBlank()) {
                continue;
            }
            sapByOur.merge(code, c.getAmount(), BigDecimal::add);
        }

        Map<String, BigDecimal> shouldByOur = extractShouldComponents(bu);

        CompareResult result = SapShouldCostCompareEngine.compare(
                s.getTotalCost(),
                bu.getTotalCost(),
                sapByOur,
                shouldByOur
        );

        s.setShouldCostBuildUpId(bu.getBuildUpId());
        s.setBomNodeId(bu.getBomNodeId());
        s.setStatus("COMPARED");
        s.setComparedAt(Instant.now());
        s.setUpdatedAt(Instant.now());
        stagingRepository.save(s);

        List<ComponentVariance> lines = result.components().stream()
                .map(l -> new ComponentVariance(l.code(), l.sapAmount(), l.shouldAmount(), l.variance(), l.status()))
                .toList();

        return new CompareResponse(
                s.getStagingId(),
                result.totalStatus(),
                result.sapTotal(),
                result.shouldTotal(),
                result.absoluteVariance(),
                result.percentageVariance(),
                bu.getBuildUpId(),
                lines,
                result.dataQualityNotes()
        );
    }

    @Transactional(readOnly = true)
    public List<MappingResponse> listMappings() {
        return mappingRepository.findByActiveFlagTrueOrderBySapComponentCodeAsc().stream()
                .map(m -> new MappingResponse(
                        m.getMappingId(), m.getSapComponentCode(), m.getOurComponentCode(),
                        m.getNotes(), m.isActiveFlag()))
                .toList();
    }

    @Transactional
    public MappingResponse upsertMapping(MappingUpsert req) {
        if (req.sapComponentCode() == null || req.ourComponentCode() == null) {
            throw new IllegalArgumentException("sapComponentCode and ourComponentCode are required");
        }
        String sapCode = req.sapComponentCode().trim();
        SapComponentMapping m = mappingRepository.findBySapComponentCodeAndActiveFlagTrue(sapCode)
                .orElseGet(() -> {
                    SapComponentMapping n = new SapComponentMapping();
                    n.setMappingId(UUID.randomUUID().toString());
                    n.setSapComponentCode(sapCode);
                    n.setCreatedAt(Instant.now());
                    n.setActiveFlag(true);
                    return n;
                });
        m.setOurComponentCode(req.ourComponentCode().trim());
        m.setNotes(req.notes());
        m.setUpdatedAt(Instant.now());
        mappingRepository.save(m);
        return new MappingResponse(
                m.getMappingId(), m.getSapComponentCode(), m.getOurComponentCode(),
                m.getNotes(), m.isActiveFlag());
    }

    private Map<String, BigDecimal> extractShouldComponents(ShouldCostBuildUp bu) {
        Map<String, BigDecimal> map = new HashMap<>();
        put(map, "MATERIAL", bu.getMaterialCost());
        put(map, "PROCESS", bu.getProcessCost());
        put(map, "TOOLING", bu.getToolingCost());
        put(map, "PACKAGING_FREIGHT", bu.getPackagingFreightCost());
        put(map, "OVERHEAD", bu.getOverheadCost());
        put(map, "SGA", bu.getSgaCost());
        put(map, "PROFIT_MARGIN", bu.getProfitMargin());
        return map;
    }

    private static void put(Map<String, BigDecimal> map, String code, BigDecimal v) {
        if (v != null) {
            map.put(code, v);
        }
    }

    private SapCostEstimateStaging requireStaging(String id) {
        return stagingRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("SapCostEstimateStaging", id));
    }

    private StagingResponse toStagingResponse(SapCostEstimateStaging s, List<String> messages) {
        List<ComponentOut> comps = componentRepository.findByStagingIdOrderByComponentSeqAsc(s.getStagingId())
                .stream()
                .map(c -> new ComponentOut(
                        c.getComponentSeq(),
                        c.getSapComponentCode(),
                        c.getSapComponentName(),
                        c.getAmount(),
                        c.getMappedComponentCode()
                ))
                .toList();
        return new StagingResponse(
                s.getStagingId(),
                s.getSourceSystem(),
                s.getSapMaterial(),
                s.getSapPlant(),
                s.getCostingVariant(),
                s.getCostingVersion(),
                s.getEstimateDate(),
                s.getCurrencyCode(),
                s.getTotalCost(),
                s.getStatus(),
                s.getShouldCostBuildUpId(),
                s.getBomNodeId(),
                s.getReceivedAt(),
                s.getComparedAt(),
                comps,
                messages != null ? messages : List.of()
        );
    }
}
