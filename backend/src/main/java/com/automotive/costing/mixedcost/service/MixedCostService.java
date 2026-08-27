package com.automotive.costing.mixedcost.service;

import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.currency.dto.CurrencyDtos.ConvertRequest;
import com.automotive.costing.currency.service.CostCurrencyConverter;
import com.automotive.costing.currency.service.CurrencyService;
import com.automotive.costing.bom.entity.BomNode;
import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.mixedcost.dto.MixedCostDtos.CreateRequest;
import com.automotive.costing.mixedcost.dto.MixedCostDtos.LegRequest;
import com.automotive.costing.mixedcost.dto.MixedCostDtos.LegResponse;
import com.automotive.costing.mixedcost.dto.MixedCostDtos.MixedCostResponse;
import com.automotive.costing.mixedcost.dto.MixedCostDtos.ReplaceLegsRequest;
import com.automotive.costing.mixedcost.engine.MixedCostBlendEngine;
import com.automotive.costing.mixedcost.engine.MixedCostBlendEngine.BlendResult;
import com.automotive.costing.mixedcost.engine.MixedCostBlendEngine.LegInput;
import com.automotive.costing.mixedcost.entity.MixedCostLeg;
import com.automotive.costing.mixedcost.entity.MixedCostVersion;
import com.automotive.costing.mixedcost.repository.MixedCostLegRepository;
import com.automotive.costing.mixedcost.repository.MixedCostVersionRepository;
import com.automotive.costing.platform.user.entity.ScopeType;
import com.automotive.costing.platform.user.service.AccessScopeService;
import com.automotive.costing.rfq.entity.SupplierQuote;
import com.automotive.costing.rfq.entity.SupplierStatus;
import com.automotive.costing.rfq.repository.SupplierMasterRepository;
import com.automotive.costing.rfq.repository.SupplierQuoteRepository;
import com.automotive.costing.shouldcost.entity.ShouldCostBuildUp;
import com.automotive.costing.shouldcost.repository.ShouldCostBuildUpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MixedCostService {

    private final MixedCostVersionRepository versionRepository;
    private final TenantAccessGuard tenantAccessGuard;
    private final MixedCostLegRepository legRepository;
    private final BomNodeRepository bomNodeRepository;
    private final ShouldCostBuildUpRepository buildUpRepository;
    private final SupplierQuoteRepository quoteRepository;
    private final SupplierMasterRepository supplierMasterRepository;
    private final CurrencyService currencyService;
    private final CostCurrencyConverter costCurrencyConverter;
    private final AccessScopeService accessScopeService;

    @Transactional(readOnly = true)
    public List<MixedCostResponse> listByBomNode(String bomNodeId) {
        BomNode node = requireNode(bomNodeId);
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, node.getProgramId());
        return versionRepository.findByBomNodeIdOrderByCreatedAtDesc(bomNodeId).stream()
                .map(v -> toResponse(v, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public MixedCostResponse get(String mixedCostId) {
        MixedCostVersion version = requireVersion(mixedCostId);
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, version.getProgramId());
        return toResponse(version, null);
    }

    @Transactional
    public MixedCostResponse create(String bomNodeId, CreateRequest request) {
        BomNode node = requireNode(bomNodeId);
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, node.getProgramId());
        if (request.versionLabel() == null || request.versionLabel().isBlank()) {
            throw new IllegalArgumentException("versionLabel is required");
        }
        if (request.legs() == null || request.legs().size() < 2) {
            throw new IllegalArgumentException("At least two legs are required");
        }

        MixedCostVersion version = new MixedCostVersion();
        version.setMixedCostId(UUID.randomUUID().toString());
        version.setProgramId(node.getProgramId());
        version.setBomNodeId(bomNodeId);
        version.setVersionLabel(request.versionLabel().trim());
        version.setStatus("DRAFT");
        version.setNotes(request.notes());
        versionRepository.save(version);

        persistLegs(version.getMixedCostId(), request.legs());
        return toResponse(version, null);
    }

    @Transactional
    public MixedCostResponse replaceLegs(String mixedCostId, ReplaceLegsRequest request) {
        MixedCostVersion version = requireVersion(mixedCostId);
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, version.getProgramId());
        if ("FROZEN".equals(version.getStatus())) {
            throw new IllegalArgumentException("Cannot modify legs of a FROZEN mixed-cost version");
        }
        if (request.legs() == null || request.legs().size() < 2) {
            throw new IllegalArgumentException("At least two legs are required");
        }
        legRepository.deleteByMixedCostId(mixedCostId);
        persistLegs(mixedCostId, request.legs());
        version.setStatus("DRAFT");
        version.setBlendedUnitTotal(null);
        version.setBlendedStatus(null);
        version.setCalculatedAt(null);
        versionRepository.save(version);
        return toResponse(version, null);
    }

    @Transactional
    public MixedCostResponse calculate(String mixedCostId) {
        MixedCostVersion version = requireVersion(mixedCostId);
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, version.getProgramId());
        if ("FROZEN".equals(version.getStatus())) {
            throw new IllegalArgumentException("Cannot recalculate a FROZEN mixed-cost version");
        }

        List<MixedCostLeg> stored = legRepository.findByMixedCostIdOrderByLegSeqAsc(mixedCostId);
        List<LegInput> inputs = new ArrayList<>();
        java.time.LocalDate valuationDate = version.getCreatedAt() != null
                ? version.getCreatedAt().atZone(java.time.ZoneOffset.UTC).toLocalDate()
                : java.time.LocalDate.now();
        for (MixedCostLeg leg : stored) {
            inputs.add(resolveLeg(version.getBomNodeId(), leg, valuationDate));
        }

        BlendResult result = MixedCostBlendEngine.calculate(inputs);

        // Persist snapshots back onto legs
        for (var snap : result.legSnapshots()) {
            MixedCostLeg leg = stored.stream()
                    .filter(l -> l.getLegSeq() == snap.legSeq())
                    .findFirst()
                    .orElseThrow();
            leg.setUnitCostSnapshot(snap.unitCostSnapshot());
            leg.setCurrencySnapshot(snap.currencySnapshot());
            leg.setEligibilityStatus(snap.eligibilityStatus());
            leg.setEligibilityNotes(snap.eligibilityNotes());
            leg.setDataStatus(snap.dataStatus());
            legRepository.save(leg);
        }

        version.setRatioSumCheck(result.ratioSum());
        version.setBlendedStatus(result.blendedStatus());
        version.setBlendedUnitTotal(result.blendedUnitTotal());
        version.setCalculatedAt(Instant.now());
        if (result.ratioValid() && !result.calculateBlocked()
                && MixedCostBlendEngine.AVAILABLE.equals(result.blendedStatus())) {
            version.setStatus("CALCULATED");
        } else {
            version.setStatus("DRAFT");
        }
        versionRepository.save(version);
        return toResponse(version, result.dataQualityNotes());
    }

    @Transactional
    public MixedCostResponse freeze(String mixedCostId) {
        MixedCostVersion version = requireVersion(mixedCostId);
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, version.getProgramId());
        if (!"CALCULATED".equals(version.getStatus())) {
            throw new IllegalArgumentException("Only CALCULATED mixed-cost versions can be frozen");
        }
        if (!MixedCostBlendEngine.AVAILABLE.equals(version.getBlendedStatus())
                || version.getBlendedUnitTotal() == null) {
            throw new IllegalArgumentException("Cannot freeze when blended status is not AVAILABLE");
        }
        version.setStatus("FROZEN");
        version.setFrozenAt(Instant.now());
        versionRepository.save(version);
        return toResponse(version, List.of("Version frozen; snapshots will not change if source quotes/should-costs are edited."));
    }

    private LegInput resolveLeg(String bomNodeId, MixedCostLeg leg, java.time.LocalDate valuationDate) {
        String type = leg.getAlternativeType();
        String elig = MixedCostBlendEngine.ELIGIBLE;
        String eligNotes = null;
        String data = MixedCostBlendEngine.NOT_AVAILABLE;
        java.math.BigDecimal unit = null;
        String currency = null;
        String supplierId = leg.getSupplierId();

        if (MixedCostBlendEngine.TYPE_INTERNAL.equals(type)) {
            ShouldCostBuildUp bu = buildUpRepository.findByCustomerIdAndBuildUpId(tenantAccessGuard.requireCustomerId(), leg.getSourceRefId()).orElse(null);
            if (bu == null) {
                eligNotes = "Should-cost build-up not found";
            } else if (!bomNodeId.equals(bu.getBomNodeId())) {
                eligNotes = "Build-up does not belong to this BOM node";
                elig = MixedCostBlendEngine.NOT_ELIGIBLE;
            } else if (bu.getTotalCost() != null) {
                unit = bu.getTotalCost();
                data = MixedCostBlendEngine.AVAILABLE;
            }
        } else if (MixedCostBlendEngine.TYPE_QUOTE.equals(type)) {
            SupplierQuote quote = quoteRepository.findByCustomerIdAndQuoteId(tenantAccessGuard.requireCustomerId(), leg.getSourceRefId()).orElse(null);
            if (quote == null) {
                eligNotes = "Supplier quote not found";
            } else {
                supplierId = quote.getSupplierId();
                currency = quote.getCurrencyCode();
                if (quote.getUnitPrice() != null) {
                    unit = quote.getUnitPrice();
                    data = MixedCostBlendEngine.AVAILABLE;
                    String target = costCurrencyConverter.getBaseCurrency();
                    if (currency != null && !currency.isBlank()
                            && !currency.trim().equalsIgnoreCase(target)) {
                        try {
                            var resp = currencyService.convert(
                                    new ConvertRequest(unit, currency.trim().toUpperCase(), target, valuationDate));
                            unit = resp.convertedAmount();
                            currency = target;
                            eligNotes = (eligNotes == null ? "" : eligNotes + "; ")
                                    + "FX to " + target + " path=" + resp.conversionPath();
                        } catch (Exception ex) {
                            data = MixedCostBlendEngine.NOT_AVAILABLE;
                            unit = null;
                            eligNotes = (eligNotes == null ? "" : eligNotes + "; ")
                                    + "FX rate DATA NOT AVAILABLE " + currency + "->" + target;
                        }
                    }
                }
                var smOpt = supplierMasterRepository.findByCustomerIdAndSupplierId(tenantAccessGuard.requireCustomerId(), quote.getSupplierId());
                if (smOpt.isPresent()) {
                    SupplierStatus st = smOpt.get().getStatus();
                    if (st == SupplierStatus.BLOCKED) {
                        elig = MixedCostBlendEngine.NOT_ELIGIBLE;
                        eligNotes = "Supplier status is BLOCKED";
                    } else if (st == SupplierStatus.INACTIVE) {
                        elig = MixedCostBlendEngine.NOT_ELIGIBLE;
                        eligNotes = "Supplier status is INACTIVE";
                    }
                }
            }
        } else {
            eligNotes = "Unknown alternativeType: " + type;
            elig = MixedCostBlendEngine.NOT_ELIGIBLE;
        }

        return new LegInput(
                leg.getLegSeq(),
                type,
                leg.getSourceRefId(),
                supplierId,
                leg.getMixRatio(),
                unit,
                currency,
                elig,
                eligNotes,
                data
        );
    }

    private void persistLegs(String mixedCostId, List<LegRequest> requests) {
        int seq = 1;
        for (LegRequest req : requests) {
            if (req.alternativeType() == null || req.sourceRefId() == null || req.mixRatio() == null) {
                throw new IllegalArgumentException("Each leg requires alternativeType, sourceRefId, mixRatio");
            }
            String type = req.alternativeType().trim().toUpperCase();
            if (!MixedCostBlendEngine.TYPE_INTERNAL.equals(type)
                    && !MixedCostBlendEngine.TYPE_QUOTE.equals(type)) {
                throw new IllegalArgumentException(
                        "alternativeType must be INTERNAL_SHOULD_COST or SUPPLIER_QUOTE");
            }
            MixedCostLeg leg = new MixedCostLeg();
            leg.setMixedCostId(mixedCostId);
            leg.setLegSeq(seq++);
            leg.setAlternativeType(type);
            leg.setSourceRefId(req.sourceRefId());
            leg.setMixRatio(req.mixRatio());
            leg.setEligibilityStatus(MixedCostBlendEngine.UNKNOWN);
            leg.setDataStatus(MixedCostBlendEngine.NOT_AVAILABLE);
            if (MixedCostBlendEngine.TYPE_QUOTE.equals(type)) {
                quoteRepository.findByCustomerIdAndQuoteId(tenantAccessGuard.requireCustomerId(), req.sourceRefId()).ifPresent(q -> leg.setSupplierId(q.getSupplierId()));
            }
            legRepository.save(leg);
        }
    }

    private BomNode requireNode(String bomNodeId) {
        return bomNodeRepository.findById(bomNodeId)
                .orElseThrow(() -> ResourceNotFoundException.of("BomNode", bomNodeId));
    }

    private MixedCostVersion requireVersion(String mixedCostId) {
        return versionRepository.findById(mixedCostId)
                .orElseThrow(() -> ResourceNotFoundException.of("MixedCostVersion", mixedCostId));
    }

    private MixedCostResponse toResponse(MixedCostVersion version, List<String> notes) {
        List<MixedCostLeg> legs = legRepository.findByMixedCostIdOrderByLegSeqAsc(version.getMixedCostId());
        List<LegResponse> legResponses = legs.stream()
                .map(l -> new LegResponse(
                        l.getLegSeq(),
                        l.getAlternativeType(),
                        l.getSourceRefId(),
                        l.getSupplierId(),
                        l.getMixRatio(),
                        l.getUnitCostSnapshot(),
                        l.getCurrencySnapshot(),
                        l.getEligibilityStatus(),
                        l.getEligibilityNotes(),
                        l.getDataStatus()
                ))
                .toList();
        return new MixedCostResponse(
                version.getMixedCostId(),
                version.getProgramId(),
                version.getBomNodeId(),
                version.getVersionLabel(),
                version.getStatus(),
                version.getReportingCurrency(),
                version.getBlendedUnitTotal(),
                version.getBlendedStatus(),
                version.getRatioSumCheck(),
                version.getNotes(),
                version.getCalculatedAt(),
                version.getFrozenAt(),
                legResponses,
                notes != null ? notes : List.of()
        );
    }
}
