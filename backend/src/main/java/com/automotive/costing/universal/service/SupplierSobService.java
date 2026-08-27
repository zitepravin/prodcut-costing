package com.automotive.costing.universal.service;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.universal.dto.UniversalCostingDtos.SobRequest;
import com.automotive.costing.universal.dto.UniversalCostingDtos.SobResponse;
import com.automotive.costing.universal.entity.SupplierShareOfBusiness;
import com.automotive.costing.universal.repository.SupplierShareOfBusinessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupplierSobService {

    public static final BigDecimal HUNDRED = new BigDecimal("100.0000");
    public static final int PCT_SCALE = 4;

    private final SupplierShareOfBusinessRepository repository;
    private final TenantAccessGuard tenantAccessGuard;

    @Transactional(readOnly = true)
    public List<SobResponse> list() {
        String customerId = tenantAccessGuard.requireCustomerId();
        return repository.findByCustomerIdOrderByAllocationGroupCodeAscPriorityAsc(customerId).stream()
                .map(this::toResponse).toList();
    }

    /**
     * Create SOB row. By default group must total 100%.
     * Pass allowPartial=true for explicitly configured partial-allocation groups (total may be &lt; 100%).
     */
    @Transactional
    public SobResponse create(SobRequest request) {
        return create(request, false);
    }

    @Transactional
    public SobResponse create(SobRequest request, boolean allowPartial) {
        String customerId = tenantAccessGuard.requireCustomerId();
        tenantAccessGuard.assertNotSupportReadOnly();
        validate(request);

        Instant now = Instant.now();
        SupplierShareOfBusiness s = new SupplierShareOfBusiness();
        s.setSobId(UUID.randomUUID().toString());
        s.setCustomerId(customerId);
        s.setCostObjectId(request.costObjectId());
        s.setProductCode(request.productCode());
        s.setSupplierId(request.supplierId().trim());
        s.setAllocationPercent(request.allocationPercent().setScale(PCT_SCALE, RoundingMode.HALF_UP));
        s.setAllocationBasis(request.allocationBasis() != null
                ? request.allocationBasis().trim().toUpperCase(Locale.ROOT) : "VOLUME");
        s.setAllocationGroupCode(request.allocationGroupCode().trim().toUpperCase(Locale.ROOT));
        s.setEffectiveFrom(request.effectiveFrom());
        s.setEffectiveTo(request.effectiveTo());
        s.setStatus("ACTIVE");
        s.setPriority(request.priority() != null ? request.priority() : 1);
        s.setVersionNo(1);
        s.setCreatedAt(now);
        s.setUpdatedAt(now);
        repository.save(s);

        validateGroupTotal(customerId, s.getAllocationGroupCode(), s.getEffectiveFrom(), allowPartial);
        return toResponse(s);
    }

    @Transactional(readOnly = true)
    public List<SupplierShareOfBusiness> resolveActive(String groupCode, LocalDate asOf) {
        String customerId = tenantAccessGuard.requireCustomerId();
        return repository.findActiveForGroup(customerId, groupCode.trim().toUpperCase(Locale.ROOT), asOf);
    }

    /** Full allocation required: total must equal 100.0000%. */
    public void validateGroupTotal(String customerId, String groupCode, LocalDate asOf) {
        validateGroupTotal(customerId, groupCode, asOf, false);
    }

    /**
     * @param allowPartial when true, total must be &gt; 0 and &lt;= 100 (partial-allocation policy).
     *                     when false, total must equal 100.0000%.
     */
    public void validateGroupTotal(String customerId, String groupCode, LocalDate asOf, boolean allowPartial) {
        List<SupplierShareOfBusiness> active = repository.findActiveForGroup(customerId, groupCode, asOf);
        BigDecimal total = BigDecimal.ZERO;
        for (SupplierShareOfBusiness row : active) {
            total = total.add(row.getAllocationPercent());
        }
        total = total.setScale(PCT_SCALE, RoundingMode.HALF_UP);

        if (allowPartial) {
            if (total.compareTo(BigDecimal.ZERO) <= 0 || total.compareTo(HUNDRED) > 0) {
                throw new BusinessRuleViolationException(
                        "SOB allocation group " + groupCode + " totals " + total
                                + "% as-of " + asOf + "; partial policy requires 0 < total <= 100.0000%",
                        "SOB-PARTIAL");
            }
        } else if (total.compareTo(HUNDRED) != 0) {
            throw new BusinessRuleViolationException(
                    "SOB allocation group " + groupCode + " totals " + total
                            + "% as-of " + asOf + "; must equal 100.0000%",
                    "SOB-100");
        }
    }

    private void validate(SobRequest request) {
        if (request.supplierId() == null || request.supplierId().isBlank()) {
            throw new BusinessRuleViolationException("supplierId is required", "SOB-01");
        }
        if (request.allocationPercent() == null) {
            throw new BusinessRuleViolationException("allocationPercent is required", "SOB-02");
        }
        if (request.allocationPercent().compareTo(BigDecimal.ZERO) <= 0
                || request.allocationPercent().compareTo(HUNDRED) > 0) {
            throw new BusinessRuleViolationException(
                    "allocationPercent must be > 0 and <= 100", "SOB-03");
        }
        if (request.allocationGroupCode() == null || request.allocationGroupCode().isBlank()) {
            throw new BusinessRuleViolationException("allocationGroupCode is required", "SOB-04");
        }
        if (request.effectiveFrom() == null) {
            throw new BusinessRuleViolationException("effectiveFrom is required", "SOB-05");
        }
        if (request.effectiveTo() != null && request.effectiveTo().isBefore(request.effectiveFrom())) {
            throw new BusinessRuleViolationException(
                    "effectiveTo must be on or after effectiveFrom", "SOB-06");
        }
    }

    private SobResponse toResponse(SupplierShareOfBusiness s) {
        return new SobResponse(
                s.getSobId(), s.getCustomerId(), s.getCostObjectId(), s.getProductCode(),
                s.getSupplierId(), s.getAllocationPercent(), s.getAllocationBasis(),
                s.getAllocationGroupCode(), s.getEffectiveFrom(), s.getEffectiveTo(),
                s.getStatus(), s.getPriority(), s.getVersionNo(), s.getCreatedAt());
    }
}
