package com.automotive.costing.vendorselection.repository;

import com.automotive.costing.vendorselection.entity.VendorQuestionnaireResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VendorQuestionnaireResponseRepository extends JpaRepository<VendorQuestionnaireResponse, String> {

    java.util.Optional<com.automotive.costing.vendorselection.entity.VendorQuestionnaireResponse> findByCustomerIdAndResponseId(String customerId, String responseId);

    java.util.List<com.automotive.costing.vendorselection.entity.VendorQuestionnaireResponse> findByCustomerIdAndSupplierIdOrderByCreatedAtDesc(String customerId, String supplierId);

    List<VendorQuestionnaireResponse> findBySupplierIdOrderByRespondedAtDesc(String supplierId);
}
