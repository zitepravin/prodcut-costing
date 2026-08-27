package com.automotive.costing.vendorselection.repository;

import com.automotive.costing.vendorselection.entity.SupplierQualificationLifecycleEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierQualificationLifecycleEventRepository
        extends JpaRepository<SupplierQualificationLifecycleEvent, String> {

    List<SupplierQualificationLifecycleEvent> findBySupplierIdOrderByOccurredAtAsc(String supplierId);

    List<SupplierQualificationLifecycleEvent> findByPublishedFlagFalseOrderByOccurredAtAsc();
}
