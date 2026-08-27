package com.automotive.costing.packaging.service;

import com.automotive.costing.bom.repository.BomNodeRepository;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.masterdata.repository.PlantMasterRepository;
import com.automotive.costing.packaging.dto.PackagingLogisticsDtos.*;
import com.automotive.costing.packaging.entity.PackagingLogisticsProfile;
import com.automotive.costing.packaging.repository.PackagingLogisticsProfileRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * M07 profile CRUD for packaging & logistics cost inputs attached to a BOM node.
 */
@Service
@RequiredArgsConstructor
public class PackagingLogisticsProfileService {

    private static final String ENTITY_TYPE = "PackagingLogisticsProfile";

    private final PackagingLogisticsProfileRepository profileRepository;
    private final TenantAccessGuard tenantAccessGuard;
    private final BomNodeRepository bomNodeRepository;
    private final PlantMasterRepository plantMasterRepository;
    private final AuditService auditService;

    @Transactional
    public ProfileResponse create(CreateProfileRequest request) {
        bomNodeRepository.findByCustomerIdAndBomNodeId(tenantAccessGuard.requireCustomerId(), request.bomNodeId())
                .orElseThrow(() -> ResourceNotFoundException.of("BomNode", request.bomNodeId()));
        if (request.originPlantId() != null && !request.originPlantId().isBlank()) {
            plantMasterRepository.findByCustomerIdAndPlantId(tenantAccessGuard.requireCustomerId(), request.originPlantId())
                    .orElseThrow(() -> ResourceNotFoundException.of("PlantMaster", request.originPlantId()));
        }

        PackagingLogisticsProfile profile = new PackagingLogisticsProfile();
        profile.setCustomerId(tenantAccessGuard.requireCustomerId());
        profile.setProfileId(UUID.randomUUID().toString());
        profile.setBomNodeId(request.bomNodeId());
        profile.setProfileLabel(
                request.profileLabel() == null || request.profileLabel().isBlank()
                        ? "Standard" : request.profileLabel().trim());
        profile.setPackagingType(request.packagingType());
        profile.setPrimaryPackCostPerPiece(nz(request.primaryPackCostPerPiece()));
        profile.setSecondaryPackCostPerPiece(nz(request.secondaryPackCostPerPiece()));
        profile.setReturnableContainerCost(request.returnableContainerCost());
        profile.setContainerLifeCycles(request.containerLifeCycles());
        profile.setNetWeightKg(request.netWeightKg());
        profile.setFreightMode(request.freightMode());
        profile.setFreightRatePerKg(request.freightRatePerKg());
        profile.setFixedFreightPerUnit(request.fixedFreightPerUnit());
        profile.setDutyPct(request.dutyPct());
        profile.setOriginPlantId(blankToNull(request.originPlantId()));
        profile.setDestinationFreightZone(blankToNull(request.destinationFreightZone()));
        profile.setNotes(request.notes());
        profile.setActive(true);

        PackagingLogisticsProfile saved = profileRepository.save(profile);
        auditService.recordCreate(ENTITY_TYPE, saved.getProfileId(), toResponse(saved));
        return toResponse(saved);
    }

    @Transactional
    public ProfileResponse update(String profileId, UpdateProfileRequest request) {
        PackagingLogisticsProfile profile = profileRepository.findByCustomerIdAndProfileId(tenantAccessGuard.requireCustomerId(), profileId)
                .orElseThrow(() -> ResourceNotFoundException.of("PackagingLogisticsProfile", profileId));
        ProfileResponse before = toResponse(profile);

        if (request.profileLabel() != null && !request.profileLabel().isBlank()) {
            profile.setProfileLabel(request.profileLabel().trim());
        }
        if (request.packagingType() != null) {
            profile.setPackagingType(request.packagingType());
        }
        if (request.primaryPackCostPerPiece() != null) {
            profile.setPrimaryPackCostPerPiece(request.primaryPackCostPerPiece());
        }
        if (request.secondaryPackCostPerPiece() != null) {
            profile.setSecondaryPackCostPerPiece(request.secondaryPackCostPerPiece());
        }
        if (request.returnableContainerCost() != null) {
            profile.setReturnableContainerCost(request.returnableContainerCost());
        }
        if (request.containerLifeCycles() != null) {
            profile.setContainerLifeCycles(request.containerLifeCycles());
        }
        if (request.netWeightKg() != null) {
            profile.setNetWeightKg(request.netWeightKg());
        }
        if (request.freightMode() != null) {
            profile.setFreightMode(request.freightMode());
        }
        if (request.freightRatePerKg() != null) {
            profile.setFreightRatePerKg(request.freightRatePerKg());
        }
        if (request.fixedFreightPerUnit() != null) {
            profile.setFixedFreightPerUnit(request.fixedFreightPerUnit());
        }
        if (request.dutyPct() != null) {
            profile.setDutyPct(request.dutyPct());
        }
        if (request.originPlantId() != null) {
            if (!request.originPlantId().isBlank()) {
                plantMasterRepository.findByCustomerIdAndPlantId(tenantAccessGuard.requireCustomerId(), request.originPlantId())
                        .orElseThrow(() -> ResourceNotFoundException.of("PlantMaster", request.originPlantId()));
            }
            profile.setOriginPlantId(blankToNull(request.originPlantId()));
        }
        if (request.destinationFreightZone() != null) {
            profile.setDestinationFreightZone(blankToNull(request.destinationFreightZone()));
        }
        if (request.notes() != null) {
            profile.setNotes(request.notes());
        }
        if (request.active() != null) {
            profile.setActive(request.active());
        }

        PackagingLogisticsProfile saved = profileRepository.save(profile);
        auditService.recordUpdate(ENTITY_TYPE, saved.getProfileId(), before, toResponse(saved));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProfileResponse getById(String profileId) {
        return toResponse(profileRepository.findByCustomerIdAndProfileId(tenantAccessGuard.requireCustomerId(), profileId)
                .orElseThrow(() -> ResourceNotFoundException.of("PackagingLogisticsProfile", profileId)));
    }

    @Transactional(readOnly = true)
    public List<ProfileResponse> listByBomNode(String bomNodeId) {
        bomNodeRepository.findByCustomerIdAndBomNodeId(tenantAccessGuard.requireCustomerId(), bomNodeId)
                .orElseThrow(() -> ResourceNotFoundException.of("BomNode", bomNodeId));
        return profileRepository.findByCustomerIdAndBomNodeIdOrderByCreatedAtDesc(tenantAccessGuard.requireCustomerId(), bomNodeId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ProfileResponse deactivate(String profileId) {
        PackagingLogisticsProfile profile = profileRepository.findByCustomerIdAndProfileId(tenantAccessGuard.requireCustomerId(), profileId)
                .orElseThrow(() -> ResourceNotFoundException.of("PackagingLogisticsProfile", profileId));
        ProfileResponse before = toResponse(profile);
        profile.setActive(false);
        PackagingLogisticsProfile saved = profileRepository.save(profile);
        auditService.recordUpdate(ENTITY_TYPE, saved.getProfileId(), before, toResponse(saved));
        return toResponse(saved);
    }

    private ProfileResponse toResponse(PackagingLogisticsProfile p) {
        return new ProfileResponse(
                p.getProfileId(),
                p.getBomNodeId(),
                p.getProfileLabel(),
                p.getPackagingType(),
                p.getPrimaryPackCostPerPiece(),
                p.getSecondaryPackCostPerPiece(),
                p.getReturnableContainerCost(),
                p.getContainerLifeCycles(),
                p.getNetWeightKg(),
                p.getFreightMode(),
                p.getFreightRatePerKg(),
                p.getFixedFreightPerUnit(),
                p.getDutyPct(),
                p.getOriginPlantId(),
                p.getDestinationFreightZone(),
                p.getNotes(),
                p.isActive());
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
