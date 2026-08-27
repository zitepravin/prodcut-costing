package com.automotive.costing.process.repository;

import com.automotive.costing.process.entity.ProcessOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcessOperationRepository extends JpaRepository<ProcessOperation, String> {

    /** PRC-01/PRC-06: operations must be processed in sequence order for a correct roll-up. */
    List<ProcessOperation> findByRoutingIdOrderBySequenceNumberAsc(String routingId);

    long countByRoutingId(String routingId);

    @Query("select count(o) from ProcessOperation o where o.customerId = :customerId "
            + "and (o.machineCode is null or o.machineCode = '' or o.laborCode is null or o.laborCode = '')")
    long countMissingMachineOrLaborByCustomerId(@Param("customerId") String customerId);

    @Query("select count(o) from ProcessOperation o where o.customerId = :customerId and o.cycleTimeSeconds <= 0")
    long countNonPositiveCycleTimeByCustomerId(@Param("customerId") String customerId);
}
