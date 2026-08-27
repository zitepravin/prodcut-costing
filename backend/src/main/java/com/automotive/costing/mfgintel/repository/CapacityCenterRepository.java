package com.automotive.costing.mfgintel.repository;
import com.automotive.costing.mfgintel.entity.CapacityCenter;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface CapacityCenterRepository extends JpaRepository<CapacityCenter, String> {
    Optional<CapacityCenter> findByCenterCodeAndPeriodCode(String centerCode, String periodCode);
    List<CapacityCenter> findByActiveTrueOrderByCenterCodeAsc();
    List<CapacityCenter> findByPeriodCodeAndActiveTrueOrderByCenterCodeAsc(String periodCode);
}
