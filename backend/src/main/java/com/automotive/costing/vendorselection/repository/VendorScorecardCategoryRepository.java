package com.automotive.costing.vendorselection.repository;

import com.automotive.costing.vendorselection.entity.VendorScorecardCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VendorScorecardCategoryRepository extends JpaRepository<VendorScorecardCategory, String> {
    List<VendorScorecardCategory> findByTemplateIdOrderBySequenceNoAsc(String templateId);
}
