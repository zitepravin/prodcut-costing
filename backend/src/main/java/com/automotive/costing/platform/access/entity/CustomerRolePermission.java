package com.automotive.costing.platform.access.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "customer_role_permission")
public class CustomerRolePermission extends BaseAuditableEntity {
    @Id
    @Column(name = "grant_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String grantId;
    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;
    @Column(name = "role_code", length = 50, nullable = false)
    private String roleCode;
    @Column(name = "permission_code", length = 64, nullable = false)
    private String permissionCode;
    @Column(name = "allowed", nullable = false)
    private boolean allowed = true;
}
