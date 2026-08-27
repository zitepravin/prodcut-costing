package com.automotive.costing.platform.user.repository;

import com.automotive.costing.platform.user.entity.ScopeType;
import com.automotive.costing.platform.user.entity.UserAccessScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAccessScopeRepository extends JpaRepository<UserAccessScope, String> {

    List<UserAccessScope> findByUserId(String userId);

    List<UserAccessScope> findByUserIdAndScopeType(String userId, ScopeType scopeType);

    boolean existsByUserIdAndScopeType(String userId, ScopeType scopeType);
}
