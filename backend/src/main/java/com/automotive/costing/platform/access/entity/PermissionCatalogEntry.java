package com.automotive.costing.platform.access.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor
@Entity
@Table(name = "permission_catalog_entry")
public class PermissionCatalogEntry {
    @Id
    @Column(name = "permission_code", length = 64, nullable = false)
    private String permissionCode;
    @Column(name = "module_code", length = 64, nullable = false)
    private String moduleCode;
    @Column(name = "feature_code", length = 64, nullable = false)
    private String featureCode;
    @Column(name = "function_code", length = 64, nullable = false)
    private String functionCode;
    @Column(name = "function_name", length = 200, nullable = false)
    private String functionName;
    @Column(name = "description", length = 500)
    private String description;
    @Column(name = "active", nullable = false)
    private boolean active = true;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
