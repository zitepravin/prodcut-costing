package com.automotive.costing.masterdata.service;

import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.masterdata.dto.VehicleProgramDtos.Response;
import com.automotive.costing.masterdata.entity.VehicleProgram;
import com.automotive.costing.masterdata.repository.VehicleProgramRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.platform.user.entity.ScopeType;
import com.automotive.costing.platform.user.service.AccessScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Vehicle programs are tenant-owned. All list/get paths filter by CustomerContext.
 */
@Service
@RequiredArgsConstructor
public class VehicleProgramService {

    private final VehicleProgramRepository repository;
    private final AccessScopeService accessScopeService;
    private final TenantAccessGuard tenantAccessGuard;

    @Transactional(readOnly = true)
    public List<Response> listAll() {
        String customerId = tenantAccessGuard.requireCustomerId();
        var restricted = accessScopeService.restrictedValuesForCurrentUser(ScopeType.PROGRAM);
        return repository.findByCustomerId(customerId).stream()
                .filter(p -> restricted.isEmpty() || restricted.get().contains(p.getProgramId())
                        || restricted.get().contains(p.getProgramCode()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Response getById(String programId) {
        String customerId = tenantAccessGuard.requireCustomerId();
        VehicleProgram entity = repository.findByCustomerIdAndProgramId(customerId, programId)
                .orElseThrow(() -> ResourceNotFoundException.of("VehicleProgram", programId));
        // Defense in depth — assert owns even after filtered query
        tenantAccessGuard.assertOwns(entity.getCustomerId(), "VehicleProgram", programId);
        accessScopeService.assertAccessForCurrentUser(ScopeType.PROGRAM, entity.getProgramId());
        return toResponse(entity);
    }

    private Response toResponse(VehicleProgram e) {
        return new Response(
                e.getProgramId(),
                e.getProgramCode(),
                e.getModelName(),
                e.getPlatformCode(),
                e.getSourcingRegion(),
                e.getLifetimeVolume(),
                e.getTargetCost(),
                e.getTargetCurrency(),
                e.getStatus()
        );
    }
}
