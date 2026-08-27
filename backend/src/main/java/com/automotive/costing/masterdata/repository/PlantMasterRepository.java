package com.automotive.costing.masterdata.repository;

import com.automotive.costing.masterdata.entity.PlantMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlantMasterRepository extends JpaRepository<PlantMaster, String> {

    Optional<PlantMaster> findByCustomerIdAndPlantId(String customerId, String plantId);

    Optional<PlantMaster> findByCustomerIdAndPlantCode(String customerId, String plantCode);

    boolean existsByCustomerIdAndPlantCode(String customerId, String plantCode);

    List<PlantMaster> findByCustomerIdOrderByPlantCodeAsc(String customerId);

    long countByCustomerId(String customerId);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    boolean existsByPlantCode(String plantCode);
}
