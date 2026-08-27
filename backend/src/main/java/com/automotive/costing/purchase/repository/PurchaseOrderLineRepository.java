package com.automotive.costing.purchase.repository;

import com.automotive.costing.purchase.entity.PurchaseOrderLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

public interface PurchaseOrderLineRepository extends JpaRepository<PurchaseOrderLine, String> {

    List<PurchaseOrderLine> findByPurchaseOrderIdOrderByLineNoAsc(String purchaseOrderId);

    void deleteByPurchaseOrderId(String purchaseOrderId);

    @Query("select coalesce(sum(l.unitPrice * l.quantity), 0) from PurchaseOrderLine l, PurchaseOrder p "
            + "where l.purchaseOrderId = p.purchaseOrderId and p.customerId = :customerId and p.status in :statuses")
    BigDecimal sumLineValueByCustomerIdAndStatusIn(
            @Param("customerId") String customerId, @Param("statuses") Collection<String> statuses);
}
