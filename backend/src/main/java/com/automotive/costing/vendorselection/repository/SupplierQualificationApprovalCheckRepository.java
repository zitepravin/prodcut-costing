package com.automotive.costing.vendorselection.repository;

import com.automotive.costing.vendorselection.entity.SupplierQualificationApprovalCheck;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierQualificationApprovalCheckRepository
        extends JpaRepository<SupplierQualificationApprovalCheck, String> {

    List<SupplierQualificationApprovalCheck> findBySupplierIdOrderByCheckedAtDesc(String supplierId);
}
