package com.automotive.costing.vendorselection.repository;

import com.automotive.costing.vendorselection.entity.VendorQuestionnaireEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VendorQuestionnaireEvaluationRepository extends JpaRepository<VendorQuestionnaireEvaluation, String> {

    java.util.Optional<com.automotive.costing.vendorselection.entity.VendorQuestionnaireEvaluation> findByCustomerIdAndEvaluationId(String customerId, String evaluationId);

    java.util.List<com.automotive.costing.vendorselection.entity.VendorQuestionnaireEvaluation> findByCustomerIdAndSupplierIdOrderByEvaluatedAtDesc(String customerId, String supplierId);

    List<VendorQuestionnaireEvaluation> findBySupplierIdOrderByEvaluatedAtDesc(String supplierId);

    Optional<VendorQuestionnaireEvaluation> findFirstBySupplierIdOrderByEvaluatedAtDesc(String supplierId);
}
