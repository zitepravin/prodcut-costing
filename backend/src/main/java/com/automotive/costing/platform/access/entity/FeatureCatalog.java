package com.automotive.costing.platform.access.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "feature_catalog")
public class FeatureCatalog {

    @Id
    @Column(name = "feature_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String featureId;

    @Column(name = "feature_code", length = 64, nullable = false, unique = true)
    private String featureCode;

    @Column(name = "feature_name", length = 200, nullable = false)
    private String featureName;

    @Column(name = "module_code", length = 64, nullable = false)
    private String moduleCode;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
