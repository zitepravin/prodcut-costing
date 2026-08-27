package com.automotive.costing.platform.user.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.platform.user.dto.ConfidentialityWallDtos.CreateRequest;
import com.automotive.costing.platform.user.dto.ConfidentialityWallDtos.Response;
import com.automotive.costing.platform.user.entity.ConfidentialityWall;
import com.automotive.costing.platform.user.repository.ConfidentialityWallRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** SEC-04: confidentiality wall configuration. See {@code ConfidentialityWall}'s Javadoc for what is and isn't built yet. */
@Service
@RequiredArgsConstructor
public class ConfidentialityWallService {

    private final ConfidentialityWallRepository repository;
    private final AuditService auditService;

    @Transactional
    public Response create(CreateRequest request) {
        if ((request.allowedRoleCode() == null || request.allowedRoleCode().isBlank())
                && (request.allowedUserId() == null || request.allowedUserId().isBlank())) {
            throw new BusinessRuleViolationException(
                    "At least one of allowedRoleCode or allowedUserId must be set.", "SEC-04");
        }

        ConfidentialityWall wall = new ConfidentialityWall();
        wall.setWallId(UUID.randomUUID().toString());
        wall.setRfqEventReference(request.rfqEventReference());
        wall.setAllowedRoleCode(request.allowedRoleCode());
        wall.setAllowedUserId(request.allowedUserId());
        wall.setCreatedAt(Instant.now());

        ConfidentialityWall saved = repository.save(wall);
        auditService.recordCreate("ConfidentialityWall", saved.getWallId(), toResponse(saved));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<Response> byRfqEvent(String rfqEventReference) {
        return repository.findByRfqEventReference(rfqEventReference).stream().map(this::toResponse).toList();
    }

    private Response toResponse(ConfidentialityWall w) {
        return new Response(w.getWallId(), w.getRfqEventReference(), w.getAllowedRoleCode(), w.getAllowedUserId());
    }
}
