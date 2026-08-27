package com.automotive.costing.platform.access.repository;

import com.automotive.costing.platform.access.entity.FeatureCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeatureCatalogRepository extends JpaRepository<FeatureCatalog, String> {
    Optional<FeatureCatalog> findByFeatureCodeIgnoreCase(String featureCode);
    List<FeatureCatalog> findByModuleCodeAndActiveTrue(String moduleCode);
    List<FeatureCatalog> findByActiveTrue();
}
