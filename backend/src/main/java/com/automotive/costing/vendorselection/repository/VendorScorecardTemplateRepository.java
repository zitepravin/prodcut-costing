package com.automotive.costing.vendorselection.repository;

import com.automotive.costing.vendorselection.entity.ScorecardTemplateStatus;
import com.automotive.costing.vendorselection.entity.VendorScorecardTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VendorScorecardTemplateRepository extends JpaRepository<VendorScorecardTemplate, String> {

    Optional<VendorScorecardTemplate> findByCustomerIdAndTemplateId(String customerId, String templateId);

    Optional<VendorScorecardTemplate> findByCustomerIdAndTemplateCodeIgnoreCase(String customerId, String templateCode);

    boolean existsByCustomerIdAndTemplateCodeIgnoreCase(String customerId, String templateCode);

    List<VendorScorecardTemplate> findByCustomerIdOrderByTemplateCodeAsc(String customerId);

    List<VendorScorecardTemplate> findByCustomerIdAndStatusOrderByTemplateCodeAsc(String customerId, ScorecardTemplateStatus status);

    long countByCustomerIdAndStatus(String customerId, ScorecardTemplateStatus status);

    List<VendorScorecardTemplate> findByCustomerIdAndLineageRootIdOrderByVersionNoAsc(String customerId, String lineageRootId);

    Optional<VendorScorecardTemplate> findFirstByCustomerIdAndSystemDefaultTrueAndStatus(
            String customerId, ScorecardTemplateStatus status);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    Optional<VendorScorecardTemplate> findByTemplateCodeIgnoreCase(String templateCode);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    boolean existsByTemplateCodeIgnoreCase(String templateCode);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<VendorScorecardTemplate> findByLineageRootIdOrderByVersionNoAsc(String lineageRootId);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    int countByLineageRootId(String lineageRootId);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    long countByStatus(ScorecardTemplateStatus status);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    Optional<VendorScorecardTemplate> findFirstBySystemDefaultTrueAndStatus(ScorecardTemplateStatus status);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<VendorScorecardTemplate> findByStatusOrderByTemplateCodeAsc(ScorecardTemplateStatus status);
}
