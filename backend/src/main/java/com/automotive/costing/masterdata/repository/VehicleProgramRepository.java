package com.automotive.costing.masterdata.repository;

import com.automotive.costing.masterdata.entity.VehicleProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleProgramRepository extends JpaRepository<VehicleProgram, String> {

    List<VehicleProgram> findByCustomerId(String customerId);

    Optional<VehicleProgram> findByCustomerIdAndProgramId(String customerId, String programId);

    Optional<VehicleProgram> findByCustomerIdAndProgramCode(String customerId, String programCode);

    boolean existsByCustomerIdAndProgramCode(String customerId, String programCode);

    /** @deprecated Prefer findByCustomerIdAndProgramCode — global code is not tenant-safe. */
    @Deprecated
    Optional<VehicleProgram> findByProgramCode(String programCode);

    /** @deprecated Prefer existsByCustomerIdAndProgramCode. */
    @Deprecated
    boolean existsByProgramCode(String programCode);

    long countByCustomerId(String customerId);
}
