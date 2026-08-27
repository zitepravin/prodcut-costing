package com.automotive.costing.platform.access.repository;

import com.automotive.costing.platform.access.entity.LicenseHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LicenseHistoryRepository extends JpaRepository<LicenseHistory, String> {
    List<LicenseHistory> findByCustomerIdOrderByCreatedAtDesc(String customerId);
}
