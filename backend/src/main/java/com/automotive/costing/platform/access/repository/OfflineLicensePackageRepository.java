package com.automotive.costing.platform.access.repository;

import com.automotive.costing.platform.access.entity.OfflineLicensePackage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OfflineLicensePackageRepository extends JpaRepository<OfflineLicensePackage, String> {
    List<OfflineLicensePackage> findByCustomerIdOrderByImportedAtDesc(String customerId);
}
