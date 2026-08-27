package com.automotive.costing.masterdata.repository;

import com.automotive.costing.masterdata.entity.OverheadPool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OverheadPoolRepository extends JpaRepository<OverheadPool, String> {

    boolean existsByCustomerIdAndCode(String customerId, String code);

    Optional<OverheadPool> findFirstByCustomerIdAndCodeOrderByVersionNumberDesc(String customerId, String code);

    Optional<OverheadPool> findByCustomerIdAndRecordId(String customerId, String recordId);

    List<OverheadPool> findByCustomerIdAndPoolCategory(String customerId, String poolCategory);

    long countByCustomerIdAndPoolCategoryIn(String customerId, List<String> poolCategories);

    List<OverheadPool> findByCustomerIdOrderByCodeAsc(String customerId);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    boolean existsByCode(String code);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    Optional<OverheadPool> findFirstByCodeOrderByVersionNumberDesc(String code);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<OverheadPool> findByPoolCategory(String poolCategory);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    long countByPoolCategoryIn(List<String> poolCategories);
}
