package com.automotive.costing.platform.access.repository;

import com.automotive.costing.platform.access.entity.PlatformCustomer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlatformCustomerRepository extends JpaRepository<PlatformCustomer, String> {
    Optional<PlatformCustomer> findByCustomerCodeIgnoreCase(String customerCode);
    boolean existsByCustomerCodeIgnoreCase(String customerCode);
}
