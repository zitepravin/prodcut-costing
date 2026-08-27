package com.automotive.costing.masterdata.repository;

import com.automotive.costing.masterdata.entity.MachineMaster;
import com.automotive.costing.masterdata.entity.MasterDataStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MachineMasterRepository extends JpaRepository<MachineMaster, String> {

    Optional<MachineMaster> findByCustomerIdAndRecordId(String customerId, String recordId);

    boolean existsByCustomerIdAndCode(String customerId, String code);

    Optional<MachineMaster> findFirstByCustomerIdAndCodeOrderByVersionNumberDesc(String customerId, String code);

    Optional<MachineMaster> findFirstByCustomerIdAndCodeAndStatusOrderByVersionNumberDesc(
            String customerId, String code, MasterDataStatus status);


    List<MachineMaster> findByCustomerIdAndCodeOrderByVersionNumberDesc(String customerId, String code);


    boolean existsByCode(String code);

    Optional<MachineMaster> findFirstByCodeOrderByVersionNumberDesc(String code);

    List<MachineMaster> findByCodeOrderByVersionNumberDesc(String code);

    /** SRS 4.5.5: machine needs at least one approved, active version for process costing. */
    Optional<MachineMaster> findFirstByCodeAndStatusOrderByVersionNumberDesc(String code, MasterDataStatus status);

    boolean existsByCodeAndStatus(String code, MasterDataStatus status);

    @Query("SELECT m FROM MachineMaster m WHERE " +
           "(:status IS NULL OR m.status = :status) AND " +
           "(:plantId IS NULL OR m.plantId = :plantId) AND " +
           "(:search IS NULL OR LOWER(m.code) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(m.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<MachineMaster> search(@Param("status") MasterDataStatus status,
                                @Param("plantId") String plantId,
                                @Param("search") String search,
                                Pageable pageable);

    long countByStatus(MasterDataStatus status);

    @Query("""
            SELECT m FROM MachineMaster m
            WHERE m.code = :code
              AND m.status = com.automotive.costing.masterdata.entity.MasterDataStatus.ACTIVE
              AND m.effectiveFrom <= :asOf
              AND (m.effectiveTo IS NULL OR m.effectiveTo >= :asOf)
            ORDER BY m.versionNumber DESC
            """)
    List<MachineMaster> findEffectiveCandidates(@Param("code") String code, @Param("asOf") LocalDate asOf);

    default Optional<MachineMaster> findEffectiveAsOf(String code, LocalDate asOf) {
        List<MachineMaster> list = findEffectiveCandidates(code, asOf);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}
