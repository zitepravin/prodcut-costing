package com.automotive.costing.platform.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A role in the RBAC model (SEC-01). Seeded roles correspond to SRS Section 2.3's
 * User Classes: MASTER_DATA_ADMIN, COSTING_ENGINEER, COSTING_MANAGER, FINANCE_CONTROLLER,
 * PURCHASING_BUYER, SOURCING_LEAD, PROGRAM_COST_MANAGER, PROCESS_ENGINEER, TOOLING_ENGINEER,
 * SYSTEM_ADMIN, plus custom roles an administrator may create.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "role")
public class Role {

    @Id
    @Column(name = "role_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String roleId;

    @Column(name = "role_code", length = 50, nullable = false, unique = true)
    private String roleCode;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "is_system_role", nullable = false)
    private boolean systemRole;
}
