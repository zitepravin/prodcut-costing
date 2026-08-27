package com.automotive.costing.platform.access.repository;

import com.automotive.costing.platform.access.entity.CustomerUserMembership;
import com.automotive.costing.platform.access.entity.MembershipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerUserMembershipRepository extends JpaRepository<CustomerUserMembership, String> {
    List<CustomerUserMembership> findByUserIdAndStatus(String userId, MembershipStatus status);
    List<CustomerUserMembership> findByCustomerId(String customerId);
    Optional<CustomerUserMembership> findByCustomerIdAndUserId(String customerId, String userId);
    boolean existsByCustomerIdAndUserIdAndStatus(String customerId, String userId, MembershipStatus status);

    long countByCustomerIdAndStatus(String customerId, MembershipStatus status);

    /**
     * Count active customer members that hold the given role code.
     * Joins membership → user → roles (native for clarity / lock-friendly counts).
     */
    @Query(value = """
            SELECT COUNT(DISTINCT m.user_id) FROM customer_user_membership m
            INNER JOIN user_role ur ON ur.user_id = m.user_id
            INNER JOIN role r ON r.role_id = ur.role_id
            WHERE m.customer_id = :customerId
              AND m.status = :status
              AND r.role_code = :roleCode
            """, nativeQuery = true)
    long countActiveMembersWithRole(
            @Param("customerId") String customerId,
            @Param("status") String status,
            @Param("roleCode") String roleCode);
}
