package com.automotive.costing.importcosting.repository;
import com.automotive.costing.importcosting.entity.ImportShipmentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ImportShipmentItemRepository extends JpaRepository<ImportShipmentItem, String> {
  List<ImportShipmentItem> findByCustomerIdAndShipmentIdOrderByLineNoAsc(String customerId, String shipmentId);
}
