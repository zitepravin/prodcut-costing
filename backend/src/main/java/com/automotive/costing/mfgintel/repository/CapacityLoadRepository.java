package com.automotive.costing.mfgintel.repository;
import com.automotive.costing.mfgintel.entity.CapacityLoad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CapacityLoadRepository extends JpaRepository<CapacityLoad, String> {
    List<CapacityLoad> findByCapacityCenterIdOrderByCreatedAtAsc(String capacityCenterId);
}
