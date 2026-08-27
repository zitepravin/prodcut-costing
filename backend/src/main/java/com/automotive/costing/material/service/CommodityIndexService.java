package com.automotive.costing.material.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.DuplicateResourceException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.material.dto.CommodityIndexDtos.PublishRequest;
import com.automotive.costing.material.dto.CommodityIndexDtos.Response;
import com.automotive.costing.material.entity.CommodityIndexValue;
import com.automotive.costing.material.repository.CommodityIndexValueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * MAT-02: maintains dated commodity index publications (e.g. LME aluminum, HRC steel).
 * Append-only, per CommodityIndexValue's class-level Javadoc - there is deliberately no
 * update/delete here, only publish (new row) and read.
 */
@Service
@RequiredArgsConstructor
public class CommodityIndexService {

    private static final String ENTITY_TYPE = "CommodityIndexValue";

    private final CommodityIndexValueRepository repository;
    private final AuditService auditService;

    @Transactional
    public Response publish(PublishRequest request) {
        if (repository.existsByIndexCodeAndEffectiveDate(request.indexCode(), request.effectiveDate())) {
            throw new DuplicateResourceException(
                    "An index value for '" + request.indexCode() + "' effective "
                            + request.effectiveDate() + " has already been published. "
                            + "Publish a correction as a new effective date instead of overwriting.");
        }

        CommodityIndexValue entity = new CommodityIndexValue();
        entity.setIndexValueId(UUID.randomUUID().toString());
        entity.setIndexCode(request.indexCode());
        entity.setIndexValue(request.indexValue());
        entity.setEffectiveDate(request.effectiveDate());
        entity.setSource(request.source());

        CommodityIndexValue saved = repository.save(entity);
        auditService.recordCreate(ENTITY_TYPE, saved.getIndexValueId(), toResponse(saved));
        return toResponse(saved);
    }

    /** MAT-02: the value effective as of a given date, per the "never a future value" rule. */
    @Transactional(readOnly = true)
    public CommodityIndexValue getEffectiveValue(String indexCode, LocalDate asOfDate) {
        return repository.findFirstByIndexCodeAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
                        indexCode, asOfDate)
                .orElseThrow(() -> ResourceNotFoundException.of(
                        "CommodityIndexValue for " + indexCode + " as of", asOfDate));
    }

    @Transactional(readOnly = true)
    public List<Response> history(String indexCode) {
        return repository.findByIndexCodeOrderByEffectiveDateDesc(indexCode).stream()
                .map(this::toResponse)
                .toList();
    }

    private Response toResponse(CommodityIndexValue v) {
        return new Response(v.getIndexValueId(), v.getIndexCode(), v.getIndexValue(),
                v.getEffectiveDate(), v.getSource());
    }
}
