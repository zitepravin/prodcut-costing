package com.automotive.costing.platform.access.repository;

import com.automotive.costing.platform.access.entity.CustomerLicense;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerLicenseRepository extends JpaRepository<CustomerLicense, String> {
    Optional<CustomerLicense> findByCustomerId(String customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM CustomerLicense l WHERE l.customerId = :customerId")
    Optional<CustomerLicense> findByCustomerIdForUpdate(@Param("customerId") String customerId);
}
