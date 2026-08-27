package com.automotive.costing.rfq.repository;

import com.automotive.costing.rfq.entity.PortalRfqAccessStatus;
import com.automotive.costing.rfq.entity.SupplierPortalRfqAccess;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierPortalRfqAccessRepository extends JpaRepository<SupplierPortalRfqAccess, String> {

    Optional<SupplierPortalRfqAccess> findBySupplierIdAndRfqIdAndStatus(
            String supplierId, String rfqId, PortalRfqAccessStatus status);

    List<SupplierPortalRfqAccess> findBySupplierIdAndStatusOrderByGrantedAtDesc(
            String supplierId, PortalRfqAccessStatus status);

    boolean existsBySupplierIdAndRfqIdAndStatus(String supplierId, String rfqId, PortalRfqAccessStatus status);
}
