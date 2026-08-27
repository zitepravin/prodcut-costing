package com.automotive.costing.rfq.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.DuplicateResourceException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.rfq.dto.RfqDtos.CreateSupplierRequest;
import com.automotive.costing.rfq.dto.RfqDtos.SupplierResponse;
import com.automotive.costing.rfq.entity.SupplierMaster;
import com.automotive.costing.rfq.entity.SupplierStatus;
import com.automotive.costing.rfq.repository.SupplierMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupplierMasterService {

    private static final String ENTITY = "SupplierMaster";

    private final SupplierMasterRepository repository;
    private final AuditService auditService;
    private final TenantAccessGuard tenantAccessGuard;

    @Transactional
    public SupplierResponse create(CreateSupplierRequest request) {
        String customerId = tenantAccessGuard.requireCustomerId();
        tenantAccessGuard.assertNotSupportReadOnly();
        String code = request.supplierCode().trim().toUpperCase();
        if (repository.findByCustomerIdAndSupplierCode(customerId, code).isPresent()) {
            throw new DuplicateResourceException("Supplier code already exists: " + code);
        }
        SupplierMaster s = new SupplierMaster();
        s.setSupplierId(UUID.randomUUID().toString());
        s.setCustomerId(customerId);
        s.setSupplierCode(code);
        s.setSupplierName(request.supplierName().trim());
        s.setCountryCode(blankToNull(request.countryCode()));
        s.setCurrencyCode(request.currencyCode() == null || request.currencyCode().isBlank()
                ? "USD" : request.currencyCode().trim().toUpperCase());
        s.setStatus(SupplierStatus.ACTIVE);
        s.setNotes(request.notes());
        SupplierMaster saved = repository.save(s);
        auditService.recordCreate(ENTITY, saved.getSupplierId(), toResponse(saved));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public SupplierResponse get(String supplierId) {
        return toResponse(require(supplierId));
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> listActive() {
        String customerId = tenantAccessGuard.requireCustomerId();
        return repository.findByCustomerIdAndStatusOrderBySupplierCodeAsc(customerId, SupplierStatus.ACTIVE).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SupplierResponse setStatus(String supplierId, SupplierStatus status) {
        tenantAccessGuard.assertNotSupportReadOnly();
        SupplierMaster s = require(supplierId);
        s.setStatus(status);
        return toResponse(repository.save(s));
    }

    public SupplierMaster require(String supplierId) {
        String customerId = tenantAccessGuard.requireCustomerId();
        SupplierMaster s = repository.findByCustomerIdAndSupplierId(customerId, supplierId)
                .orElseThrow(() -> ResourceNotFoundException.of(ENTITY, supplierId));
        tenantAccessGuard.assertOwns(s.getCustomerId(), ENTITY, supplierId);
        return s;
    }

    private SupplierResponse toResponse(SupplierMaster s) {
        return new SupplierResponse(
                s.getSupplierId(), s.getSupplierCode(), s.getSupplierName(),
                s.getCountryCode(), s.getCurrencyCode(), s.getStatus(), s.getNotes());
    }

    private static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }
}
