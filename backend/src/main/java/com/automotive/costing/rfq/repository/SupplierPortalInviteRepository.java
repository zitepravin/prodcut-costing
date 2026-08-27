package com.automotive.costing.rfq.repository;

import com.automotive.costing.rfq.entity.SupplierPortalInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SupplierPortalInviteRepository extends JpaRepository<SupplierPortalInvite, String> {
    Optional<SupplierPortalInvite> findByTokenHash(String tokenHash);
}
