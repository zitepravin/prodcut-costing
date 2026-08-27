package com.automotive.costing.platform.access.repository;

import com.automotive.costing.platform.access.entity.CustomerRoleLimit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRoleLimitRepository extends JpaRepository<CustomerRoleLimit, String> {
    List<CustomerRoleLimit> findByCustomerId(String customerId);

    Optional<CustomerRoleLimit> findByCustomerIdAndRoleCode(String customerId, String roleCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM CustomerRoleLimit r WHERE r.customerId = :customerId AND r.roleCode = :roleCode")
    Optional<CustomerRoleLimit> findByCustomerIdAndRoleCodeForUpdate(
            @Param("customerId") String customerId, @Param("roleCode") String roleCode);
}
