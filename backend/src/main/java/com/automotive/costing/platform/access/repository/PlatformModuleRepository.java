package com.automotive.costing.platform.access.repository;

import com.automotive.costing.platform.access.entity.PlatformModule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlatformModuleRepository extends JpaRepository<PlatformModule, String> {
    Optional<PlatformModule> findByModuleCodeIgnoreCase(String moduleCode);
}
