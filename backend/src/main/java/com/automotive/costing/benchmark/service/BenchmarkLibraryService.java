package com.automotive.costing.benchmark.service;

import com.automotive.costing.benchmark.dto.BenchmarkDtos.*;
import com.automotive.costing.benchmark.entity.*;
import com.automotive.costing.benchmark.repository.BenchmarkPackRepository;
import com.automotive.costing.benchmark.repository.BenchmarkRateRepository;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BenchmarkLibraryService {

    private static final int SCALE = 6;

    private final BenchmarkPackRepository packRepository;
    private final BenchmarkRateRepository rateRepository;

    @Transactional(readOnly = true)
    public List<PackResponse> listPacks() {
        return packRepository.findAllByOrderByPackCodeAscVersionLabelAsc().stream()
                .map(this::toPack)
                .toList();
    }

    @Transactional(readOnly = true)
    public PackDetailResponse getPack(String packId) {
        BenchmarkPack pack = requirePack(packId);
        List<RateResponse> rates = rateRepository.findByPackIdOrderByCategoryAscRateCodeAsc(packId).stream()
                .map(this::toRate)
                .toList();
        return new PackDetailResponse(toPack(pack), rates);
    }

    @Transactional
    public PackResponse createPack(CreatePackRequest req) {
        String version = blankTo(req.versionLabel(), "1.0");
        packRepository.findByPackCodeAndVersionLabel(req.packCode().trim(), version).ifPresent(p -> {
            throw new BusinessRuleViolationException(
                    "Pack code+version already exists: " + req.packCode() + " / " + version,
                    "BENCHMARK_PACK_EXISTS");
        });
        BenchmarkPack pack = new BenchmarkPack();
        pack.setPackId(UUID.randomUUID().toString());
        pack.setPackCode(req.packCode().trim());
        pack.setPackName(req.packName().trim());
        pack.setRegionCode(req.regionCode().trim());
        pack.setSourceLabel(trimToNull(req.sourceLabel()));
        pack.setVersionLabel(version);
        pack.setStatus(BenchmarkPackStatus.DRAFT);
        pack.setEffectiveFrom(req.effectiveFrom());
        pack.setEffectiveTo(req.effectiveTo());
        pack.setNotes(trimToNull(req.notes()));
        return toPack(packRepository.save(pack));
    }

    @Transactional
    public PackResponse publish(String packId) {
        BenchmarkPack pack = requirePack(packId);
        if (pack.getStatus() == BenchmarkPackStatus.RETIRED) {
            throw new BusinessRuleViolationException("Cannot publish a retired pack", "BENCHMARK_PACK_RETIRED");
        }
        if (rateRepository.countByPackId(packId) == 0) {
            throw new BusinessRuleViolationException("Publish requires at least one rate", "BENCHMARK_PACK_EMPTY");
        }
        pack.setStatus(BenchmarkPackStatus.PUBLISHED);
        return toPack(packRepository.save(pack));
    }

    @Transactional
    public RateResponse addRate(String packId, CreateRateRequest req) {
        BenchmarkPack pack = requirePack(packId);
        if (pack.getStatus() == BenchmarkPackStatus.RETIRED) {
            throw new BusinessRuleViolationException("Cannot add rates to retired pack", "BENCHMARK_PACK_RETIRED");
        }
        if (req.rateValue().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleViolationException("Rate value cannot be negative", "BENCHMARK_RATE_NEGATIVE");
        }
        BenchmarkRate rate = new BenchmarkRate();
        rate.setRateId(UUID.randomUUID().toString());
        rate.setPackId(packId);
        rate.setCategory(req.category());
        rate.setProcessFamily(trimToNull(req.processFamily()));
        rate.setRateCode(req.rateCode().trim());
        rate.setDescription(trimToNull(req.description()));
        rate.setRateValue(req.rateValue().setScale(SCALE, RoundingMode.HALF_UP));
        rate.setCurrencyCode(req.currencyCode().trim().toUpperCase());
        rate.setUnitOfMeasure(req.unitOfMeasure().trim());
        rate.setConfidence(blankTo(req.confidence(), "MEDIUM"));
        return toRate(rateRepository.save(rate));
    }

    private BenchmarkPack requirePack(String packId) {
        return packRepository.findById(packId)
                .orElseThrow(() -> ResourceNotFoundException.of("BenchmarkPack", packId));
    }

    private PackResponse toPack(BenchmarkPack p) {
        return new PackResponse(
                p.getPackId(), p.getPackCode(), p.getPackName(), p.getRegionCode(),
                p.getSourceLabel(), p.getVersionLabel(), p.getStatus(),
                p.getEffectiveFrom(), p.getEffectiveTo(), p.getNotes(),
                rateRepository.countByPackId(p.getPackId()),
                p.getUpdatedAt()
        );
    }

    private RateResponse toRate(BenchmarkRate r) {
        return new RateResponse(
                r.getRateId(), r.getPackId(), r.getCategory(), r.getProcessFamily(),
                r.getRateCode(), r.getDescription(), r.getRateValue(),
                r.getCurrencyCode(), r.getUnitOfMeasure(), r.getConfidence()
        );
    }

    private static String blankTo(String v, String def) {
        return v == null || v.isBlank() ? def : v.trim();
    }

    private static String trimToNull(String v) {
        if (v == null || v.isBlank()) return null;
        return v.trim();
    }
}
