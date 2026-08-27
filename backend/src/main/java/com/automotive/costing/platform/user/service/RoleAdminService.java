package com.automotive.costing.platform.user.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.DuplicateResourceException;
import com.automotive.costing.platform.user.dto.AdminDtos.CreateRoleRequest;
import com.automotive.costing.platform.user.dto.AdminDtos.RoleResponse;
import com.automotive.costing.platform.user.entity.Role;
import com.automotive.costing.platform.user.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** SEC-01: "System shall support role-based access control with at least eight predefined roles and the ability to create custom roles." The 8+ predefined roles are seeded across V1/V4/V5/V6/V7; this service is the "create custom roles" half. */
@Service
@RequiredArgsConstructor
public class RoleAdminService {

    private static final String ENTITY_TYPE = "Role";

    private final RoleRepository roleRepository;
    private final AuditService auditService;

    @Transactional
    public RoleResponse createCustomRole(CreateRoleRequest request) {
        if (roleRepository.findByRoleCode(request.roleCode()).isPresent()) {
            throw new DuplicateResourceException("A role with code '" + request.roleCode() + "' already exists.");
        }

        Role role = Role.builder()
                .roleId(UUID.randomUUID().toString())
                .roleCode(request.roleCode())
                .description(request.description())
                .systemRole(false) // administrator-created, distinct from the seeded system roles
                .build();

        Role saved = roleRepository.save(role);
        auditService.recordCreate(ENTITY_TYPE, saved.getRoleId(), toResponse(saved));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> list() {
        return roleRepository.findAll().stream().map(this::toResponse).toList();
    }

    private RoleResponse toResponse(Role r) {
        return new RoleResponse(r.getRoleId(), r.getRoleCode(), r.getDescription(), r.isSystemRole());
    }
}
