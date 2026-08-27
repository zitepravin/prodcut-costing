package com.automotive.costing.packaging.repository;

import com.automotive.costing.packaging.entity.PackagingLogisticsProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PackagingLogisticsProfileRepository extends JpaRepository<PackagingLogisticsProfile, String> {

    Optional<PackagingLogisticsProfile> findByCustomerIdAndProfileId(String customerId, String profileId);

    List<PackagingLogisticsProfile> findByCustomerIdAndBomNodeIdOrderByCreatedAtDesc(String customerId, String bomNodeId);

    long countByCustomerId(String customerId);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    Optional<PackagingLogisticsProfile> findByProfileId(String profileId);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<PackagingLogisticsProfile> findByBomNodeIdOrderByCreatedAtDesc(String bomNodeId);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<PackagingLogisticsProfile> findByBomNodeIdAndActiveTrueOrderByCreatedAtDesc(String bomNodeId);
}
