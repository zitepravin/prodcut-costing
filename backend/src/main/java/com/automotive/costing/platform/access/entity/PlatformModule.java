package com.automotive.costing.platform.access.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "platform_module")
public class PlatformModule extends BaseAuditableEntity {

    @Id
    @Column(name = "module_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String moduleId;

    @Column(name = "module_code", length = 50, nullable = false)
    private String moduleCode;

    @Column(name = "module_name", length = 150, nullable = false)
    private String moduleName;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ModuleStatus status = ModuleStatus.ACTIVE;
}
