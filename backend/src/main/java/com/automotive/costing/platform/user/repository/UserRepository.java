package com.automotive.costing.platform.user.repository;

import com.automotive.costing.platform.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /**
     * Active users holding the given role code (e.g. COSTING_MANAGER).
     * Used by WF-04 escalation to reassign overdue steps to a backup/manager.
     */
    @Query("""
            select distinct u from User u
            join u.roles r
            where u.status = 'Active'
              and r.roleCode = :roleCode
            order by u.username
            """)
    List<User> findActiveByRoleCode(@Param("roleCode") String roleCode);
}
