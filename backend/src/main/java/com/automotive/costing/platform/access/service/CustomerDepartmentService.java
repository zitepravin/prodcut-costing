package com.automotive.costing.platform.access.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.DuplicateResourceException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.platform.access.CustomerContext;
import com.automotive.costing.platform.access.dto.CustomerAdminDtos.*;
import com.automotive.costing.platform.access.entity.CustomerDepartment;
import com.automotive.costing.platform.access.repository.CustomerDepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerDepartmentService {

    private final CustomerDepartmentRepository departmentRepository;
    private final AuditService auditService;

    private String requireCustomerId() {
        String cid = CustomerContext.getCustomerId();
        if (cid == null || cid.isBlank()) {
            throw new BusinessRuleViolationException(
                    "Customer context required (X-Customer-Id with active membership)", "ACCESS-10");
        }
        return cid;
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> list() {
        String customerId = requireCustomerId();
        return departmentRepository.findByCustomerIdOrderByDepartmentNameAsc(customerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DepartmentResponse get(String departmentId) {
        String customerId = requireCustomerId();
        CustomerDepartment d = departmentRepository.findByDepartmentIdAndCustomerId(departmentId, customerId)
                .orElseThrow(() -> ResourceNotFoundException.of("CustomerDepartment", departmentId));
        return toResponse(d);
    }

    @Transactional
    public DepartmentResponse create(CreateDepartmentRequest request) {
        String customerId = requireCustomerId();
        String code = request.departmentCode().trim().toUpperCase();
        if (departmentRepository.existsByCustomerIdAndDepartmentCodeIgnoreCase(customerId, code)) {
            throw new DuplicateResourceException("Department code already exists for this customer: " + code);
        }
        CustomerDepartment d = new CustomerDepartment();
        d.setDepartmentId(UUID.randomUUID().toString());
        d.setCustomerId(customerId);
        d.setDepartmentCode(code);
        d.setDepartmentName(request.departmentName().trim());
        d.setDescription(request.description() != null ? request.description().trim() : null);
        d.setStatus("ACTIVE");
        CustomerDepartment saved = departmentRepository.save(d);
        auditService.recordCreate("CustomerDepartment", saved.getDepartmentId(), toResponse(saved));
        return toResponse(saved);
    }

    @Transactional
    public DepartmentResponse update(String departmentId, UpdateDepartmentRequest request) {
        String customerId = requireCustomerId();
        CustomerDepartment d = departmentRepository.findByDepartmentIdAndCustomerId(departmentId, customerId)
                .orElseThrow(() -> ResourceNotFoundException.of("CustomerDepartment", departmentId));
        DepartmentResponse before = toResponse(d);
        if (request.departmentName() != null && !request.departmentName().isBlank()) {
            d.setDepartmentName(request.departmentName().trim());
        }
        if (request.description() != null) {
            d.setDescription(request.description().isBlank() ? null : request.description().trim());
        }
        if (request.status() != null && !request.status().isBlank()) {
            String st = request.status().trim().toUpperCase();
            if (!st.equals("ACTIVE") && !st.equals("INACTIVE")) {
                throw new BusinessRuleViolationException("status must be ACTIVE or INACTIVE", "DEPT-02");
            }
            d.setStatus(st);
        }
        CustomerDepartment saved = departmentRepository.save(d);
        auditService.recordUpdate("CustomerDepartment", departmentId, before, toResponse(saved));
        return toResponse(saved);
    }

    @Transactional
    public DepartmentResponse deactivate(String departmentId) {
        String customerId = requireCustomerId();
        CustomerDepartment d = departmentRepository.findByDepartmentIdAndCustomerId(departmentId, customerId)
                .orElseThrow(() -> ResourceNotFoundException.of("CustomerDepartment", departmentId));
        DepartmentResponse before = toResponse(d);
        d.setStatus("INACTIVE");
        CustomerDepartment saved = departmentRepository.save(d);
        auditService.recordTransition("CustomerDepartment", departmentId, before.status(), "INACTIVE");
        return toResponse(saved);
    }

    private DepartmentResponse toResponse(CustomerDepartment d) {
        return new DepartmentResponse(
                d.getDepartmentId(),
                d.getCustomerId(),
                d.getDepartmentCode(),
                d.getDepartmentName(),
                d.getDescription(),
                d.getStatus()
        );
    }
}
