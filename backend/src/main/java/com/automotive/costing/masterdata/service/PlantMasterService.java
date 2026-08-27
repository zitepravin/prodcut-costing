package com.automotive.costing.masterdata.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.DuplicateResourceException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.masterdata.entity.PlantMaster;
import com.automotive.costing.masterdata.repository.PlantMasterRepository;
import com.automotive.costing.platform.access.service.CommercialLicenseService;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Tenant-owned plant catalog (TABLE_TENANT_CLASSIFICATION).
 * Commercial plant_site_limit enforced on create.
 */
@Service
@RequiredArgsConstructor
public class PlantMasterService {

    private final PlantMasterRepository repository;
    private final TenantAccessGuard tenantAccessGuard;
    private final AuditService auditService;
    private final CommercialLicenseService commercialLicenseService;

    public record CreateRequest(String plantCode, String country, String statutoryDutyNotes, String freightZone) {}
    public record Response(String plantId, String plantCode, String country, String statutoryDutyNotes, String freightZone) {}

    @Transactional
    public Response create(CreateRequest request) {
        String customerId = tenantAccessGuard.requireCustomerId();
        commercialLicenseService.assertLicenseActiveIfLicensed(customerId);
        commercialLicenseService.assertPlantLimit(customerId, 1);

        String code = request.plantCode() == null ? null : request.plantCode().trim();
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("plantCode is required");
        }
        if (repository.existsByCustomerIdAndPlantCode(customerId, code)) {
            throw new DuplicateResourceException("Plant code already exists for this customer: " + code);
        }
        PlantMaster p = new PlantMaster();
        p.setPlantId(UUID.randomUUID().toString());
        p.setCustomerId(customerId);
        p.setPlantCode(code);
        p.setCountry(request.country());
        p.setStatutoryDutyNotes(request.statutoryDutyNotes());
        p.setFreightZone(request.freightZone());
        repository.save(p);
        auditService.recordCreate("PlantMaster", p.getPlantId(), code);
        return toResponse(p);
    }

    @Transactional(readOnly = true)
    public Response get(String plantId) {
        return toResponse(require(plantId));
    }

    @Transactional(readOnly = true)
    public List<Response> list() {
        return repository.findByCustomerIdOrderByPlantCodeAsc(tenantAccessGuard.requireCustomerId())
                .stream().map(this::toResponse).toList();
    }

    /** Used by other modules — enforces tenant ownership. */
    @Transactional(readOnly = true)
    public PlantMaster require(String plantId) {
        String customerId = tenantAccessGuard.requireCustomerId();
        PlantMaster p = repository.findByCustomerIdAndPlantId(customerId, plantId)
                .orElseThrow(() -> ResourceNotFoundException.of("PlantMaster", plantId));
        tenantAccessGuard.assertOwns(p.getCustomerId(), "PlantMaster", plantId);
        return p;
    }

    private Response toResponse(PlantMaster p) {
        return new Response(p.getPlantId(), p.getPlantCode(), p.getCountry(),
                p.getStatutoryDutyNotes(), p.getFreightZone());
    }
}
