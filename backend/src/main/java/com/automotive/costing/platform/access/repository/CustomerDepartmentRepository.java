package com.automotive.costing.platform.access.repository;

import com.automotive.costing.platform.access.entity.CustomerDepartment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerDepartmentRepository extends JpaRepository<CustomerDepartment, String> {
    List<CustomerDepartment> findByCustomerIdOrderByDepartmentNameAsc(String customerId);
    Optional<CustomerDepartment> findByDepartmentIdAndCustomerId(String departmentId, String customerId);
    boolean existsByCustomerIdAndDepartmentCodeIgnoreCase(String customerId, String departmentCode);
}
