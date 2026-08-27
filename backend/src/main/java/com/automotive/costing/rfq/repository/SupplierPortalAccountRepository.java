package com.automotive.costing.rfq.repository;

import com.automotive.costing.rfq.entity.SupplierPortalAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SupplierPortalAccountRepository extends JpaRepository<SupplierPortalAccount, String> {
    Optional<SupplierPortalAccount> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}
