package com.automotive.costing.shouldcost.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.shouldcost.dto.BenchmarkDtos.*;
import com.automotive.costing.shouldcost.entity.ParametricCostBenchmark;
import com.automotive.costing.shouldcost.entity.SgaMarginBenchmark;
import com.automotive.costing.shouldcost.repository.ParametricCostBenchmarkRepository;
import com.automotive.costing.shouldcost.repository.SgaMarginBenchmarkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Manages the two small reference-data tables M08 introduces (SRS 4.8.5's SG&amp;A/margin
 * benchmarks, and the SC-02 parametric $/kg benchmarks). Deliberately simple create-only
 * CRUD (no MDM-02-style versioning) - see {@code SgaMarginBenchmark}'s Javadoc for why.
 */
@Service
@RequiredArgsConstructor
public class BenchmarkService {

    private final SgaMarginBenchmarkRepository sgaMarginRepository;
    private final ParametricCostBenchmarkRepository parametricRepository;
    private final AuditService auditService;

    @Transactional
    public SgaMarginResponse createSgaMarginBenchmark(SgaMarginCreateRequest request) {
        SgaMarginBenchmark entity = new SgaMarginBenchmark();
        entity.setBenchmarkId(UUID.randomUUID().toString());
        entity.setCommodityFamily(request.commodityFamily());
        entity.setRegion(request.region());
        entity.setSgaPct(request.sgaPct());
        entity.setMarginPct(request.marginPct());
        entity.setActive(true);

        SgaMarginBenchmark saved = sgaMarginRepository.save(entity);
        auditService.recordCreate("SgaMarginBenchmark", saved.getBenchmarkId(), toResponse(saved));
        return toResponse(saved);
    }

    @Transactional
    public ParametricResponse createParametricBenchmark(ParametricCreateRequest request) {
        ParametricCostBenchmark entity = new ParametricCostBenchmark();
        entity.setBenchmarkId(UUID.randomUUID().toString());
        entity.setMaterialFamily(request.materialFamily());
        entity.setProcessFamily(request.processFamily());
        entity.setRegion(request.region());
        entity.setCostPerKg(request.costPerKg());
        entity.setActive(true);

        ParametricCostBenchmark saved = parametricRepository.save(entity);
        auditService.recordCreate("ParametricCostBenchmark", saved.getBenchmarkId(), toResponse(saved));
        return toResponse(saved);
    }

    private SgaMarginResponse toResponse(SgaMarginBenchmark b) {
        return new SgaMarginResponse(b.getBenchmarkId(), b.getCommodityFamily(), b.getRegion(),
                b.getSgaPct(), b.getMarginPct(), b.isActive());
    }

    private ParametricResponse toResponse(ParametricCostBenchmark b) {
        return new ParametricResponse(b.getBenchmarkId(), b.getMaterialFamily(), b.getProcessFamily(),
                b.getRegion(), b.getCostPerKg(), b.isActive());
    }
}
