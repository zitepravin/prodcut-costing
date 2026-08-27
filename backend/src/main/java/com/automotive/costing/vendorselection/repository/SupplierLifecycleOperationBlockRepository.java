package com.automotive.costing.vendorselection.repository;

import com.automotive.costing.vendorselection.entity.SupplierLifecycleOperationBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierLifecycleOperationBlockRepository
        extends JpaRepository<SupplierLifecycleOperationBlock, String> {

    List<SupplierLifecycleOperationBlock> findBySupplierIdOrderByAttemptedAtDesc(String supplierId);
}
