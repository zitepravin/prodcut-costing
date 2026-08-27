package com.automotive.costing.importcosting.repository;
import com.automotive.costing.importcosting.entity.ImportShipment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface ImportShipmentRepository extends JpaRepository<ImportShipment, String> {
  Optional<ImportShipment> findByCustomerIdAndShipmentId(String customerId, String shipmentId);
}
