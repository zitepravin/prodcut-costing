package com.automotive.costing.platform.access.repository;
import com.automotive.costing.platform.access.entity.CustomerRolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface CustomerRolePermissionRepository extends JpaRepository<CustomerRolePermission, String> {
    List<CustomerRolePermission> findByCustomerIdAndRoleCode(String customerId, String roleCode);
    Optional<CustomerRolePermission> findByCustomerIdAndRoleCodeAndPermissionCode(String customerId, String roleCode, String permissionCode);
    List<CustomerRolePermission> findByCustomerId(String customerId);
}
