package com.automotive.costing.erp.sap.repository;

import com.automotive.costing.erp.sap.entity.SapComponentMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SapComponentMappingRepository extends JpaRepository<SapComponentMapping, String> {

    Optional<SapComponentMapping> findBySapComponentCodeAndActiveFlagTrue(String sapComponentCode);

    List<SapComponentMapping> findByActiveFlagTrueOrderBySapComponentCodeAsc();
}
