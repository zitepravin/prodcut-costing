package com.automotive.costing.masterdata.repository;

import com.automotive.costing.masterdata.entity.ProcessMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcessMasterRepository extends JpaRepository<ProcessMaster, String> {

    java.util.Optional<ProcessMaster> findByCustomerIdAndProcessId(String customerId, String processId);

    boolean existsByCustomerIdAndProcessName(String customerId, String processName);

    java.util.Optional<ProcessMaster> findByCustomerIdAndProcessName(String customerId, String processName);

    java.util.List<ProcessMaster> findByCustomerIdOrderByProcessNameAsc(String customerId);

    boolean existsByProcessName(String processName);

    Optional<ProcessMaster> findByProcessName(String processName);
}
