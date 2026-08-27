package com.automotive.costing.process.repository;

import com.automotive.costing.process.entity.ProcessRouting;
import com.automotive.costing.process.entity.RoutingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessRoutingRepository extends JpaRepository<ProcessRouting, String> {

    /** PRC-07: a BOM node may have more than one routing (e.g. manual vs. automated line). */
    List<ProcessRouting> findByBomNodeId(String bomNodeId);

    Optional<ProcessRouting> findByRoutingId(String routingId);

    Optional<ProcessRouting> findByCustomerIdAndRoutingId(String customerId, String routingId);

    List<ProcessRouting> findByCustomerIdAndBomNodeId(String customerId, String bomNodeId);

    long countByCustomerIdAndStatus(String customerId, RoutingStatus status);
}
