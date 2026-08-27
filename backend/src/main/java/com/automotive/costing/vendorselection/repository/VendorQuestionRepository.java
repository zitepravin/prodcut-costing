package com.automotive.costing.vendorselection.repository;

import com.automotive.costing.vendorselection.entity.QuestionCategory;
import com.automotive.costing.vendorselection.entity.VendorQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VendorQuestionRepository extends JpaRepository<VendorQuestion, String> {

    Optional<VendorQuestion> findByCustomerIdAndQuestionId(String customerId, String questionId);

    Optional<VendorQuestion> findByCustomerIdAndQuestionCodeAndVersionNo(String customerId, String questionCode, int versionNo);

    boolean existsByCustomerIdAndQuestionCodeAndVersionNo(String customerId, String questionCode, int versionNo);

    List<VendorQuestion> findByCustomerIdAndQuestionCodeOrderByVersionNoDesc(String customerId, String questionCode);

    List<VendorQuestion> findByCustomerIdAndActiveFlagTrueOrderByCategoryAscQuestionCodeAsc(String customerId);

    List<VendorQuestion> findByCustomerIdAndCategoryAndActiveFlagTrueOrderByQuestionCodeAsc(
            String customerId, QuestionCategory category);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    Optional<VendorQuestion> findByQuestionCodeAndVersionNo(String questionCode, int versionNo);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    boolean existsByQuestionCodeAndVersionNo(String questionCode, int versionNo);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<VendorQuestion> findByActiveFlagTrueOrderByCategoryAscQuestionCodeAsc();

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<VendorQuestion> findByCategoryAndActiveFlagTrueOrderByQuestionCodeAsc(QuestionCategory category);
}
