package com.automotive.costing.rfq.repository;

import com.automotive.costing.rfq.entity.RfqEvent;
import com.automotive.costing.rfq.entity.RfqStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RfqEventRepository extends JpaRepository<RfqEvent, String> {

    Optional<RfqEvent> findByCustomerIdAndRfqId(String customerId, String rfqId);

    Optional<RfqEvent> findByCustomerIdAndRfqNumber(String customerId, String rfqNumber);

    List<RfqEvent> findByCustomerIdAndProgramIdOrderByCreatedAtDesc(String customerId, String programId);

    List<RfqEvent> findByCustomerIdAndStatusOrderByCreatedAtDesc(String customerId, RfqStatus status);

    long countByCustomerIdAndStatus(String customerId, RfqStatus status);

    long countByCustomerIdAndStatusAndDueDateBetween(
            String customerId, RfqStatus status, LocalDate fromInclusive, LocalDate toInclusive);

    @Query("select count(r) from RfqEvent r where r.customerId = :customerId and r.status = :status "
            + "and not exists (select 1 from SupplierQuote q where q.rfqId = r.rfqId)")
    long countOpenWithNoQuotes(@Param("customerId") String customerId, @Param("status") RfqStatus status);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    Optional<RfqEvent> findByRfqId(String rfqId);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    Optional<RfqEvent> findByRfqNumber(String rfqNumber);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<RfqEvent> findByProgramIdOrderByCreatedAtDesc(String programId);

    /** @deprecated Prefer tenant-scoped methods */
    @Deprecated
    List<RfqEvent> findByStatusOrderByCreatedAtDesc(RfqStatus status);
}
