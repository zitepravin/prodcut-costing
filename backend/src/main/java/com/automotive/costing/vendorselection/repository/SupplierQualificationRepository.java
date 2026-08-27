package com.automotive.costing.vendorselection.repository;

import com.automotive.costing.vendorselection.entity.SupplierQualification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierQualificationRepository extends JpaRepository<SupplierQualification, String> {
    List<SupplierQualification> findBySupplierIdOrderByEffectiveAtDesc(String supplierId);
}
