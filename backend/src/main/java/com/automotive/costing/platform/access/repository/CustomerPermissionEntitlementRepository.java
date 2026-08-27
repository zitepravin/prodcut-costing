package com.automotive.costing.platform.access.repository;
import com.automotive.costing.platform.access.entity.CustomerPermissionEntitlement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface CustomerPermissionEntitlementRepository extends JpaRepository<CustomerPermissionEntitlement, String> {
    List<CustomerPermissionEntitlement> findByCustomerId(String customerId);
    Optional<CustomerPermissionEntitlement> findByCustomerIdAndPermissionCode(String customerId, String permissionCode);
}
