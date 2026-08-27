package com.automotive.costing.masterdata.repository;

import com.automotive.costing.masterdata.entity.LaborMaster;
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
public interface LaborMasterRepository extends JpaRepository<LaborMaster, String> {

    Optional<LaborMaster> findByCustomerIdAndRecordId(String customerId, String recordId);

    boolean existsByCustomerIdAndCode(String customerId, String code);

    Optional<LaborMaster> findFirstByCustomerIdAndCodeOrderByVersionNumberDesc(String customerId, String code);

    Optional<LaborMaster> findFirstByCustomerIdAndCodeAndStatusOrderByVersionNumberDesc(
            String customerId, String code, MasterDataStatus status);


    List<LaborMaster> findByCustomerIdAndCodeOrderByVersionNumberDesc(String customerId, String code);


    boolean existsByCode(String code);

    Optional<LaborMaster> findFirstByCodeOrderByVersionNumberDesc(String code);

    List<LaborMaster> findByCodeOrderByVersionNumberDesc(String code);

    Optional<LaborMaster> findFirstByCodeAndStatusOrderByVersionNumberDesc(String code, MasterDataStatus status);

    boolean existsByCodeAndStatus(String code, MasterDataStatus status);

    @Query("SELECT m FROM LaborMaster m WHERE " +
           "(:status IS NULL OR m.status = :status) AND " +
           "(:plantId IS NULL OR m.plantId = :plantId) AND " +
           "(:search IS NULL OR LOWER(m.code) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(m.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<LaborMaster> search(@Param("status") MasterDataStatus status,
                              @Param("plantId") String plantId,
                              @Param("search") String search,
                              Pageable pageable);

    long countByStatus(MasterDataStatus status);

    @Query("""
            SELECT m FROM LaborMaster m
            WHERE m.code = :code
              AND m.status = com.automotive.costing.masterdata.entity.MasterDataStatus.ACTIVE
              AND m.effectiveFrom <= :asOf
              AND (m.effectiveTo IS NULL OR m.effectiveTo >= :asOf)
            ORDER BY m.versionNumber DESC
            """)
    List<LaborMaster> findEffectiveCandidates(@Param("code") String code, @Param("asOf") LocalDate asOf);

    default Optional<LaborMaster> findEffectiveAsOf(String code, LocalDate asOf) {
        List<LaborMaster> list = findEffectiveCandidates(code, asOf);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }
}
