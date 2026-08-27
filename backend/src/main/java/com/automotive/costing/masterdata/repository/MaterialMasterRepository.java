package com.automotive.costing.masterdata.repository;

import com.automotive.costing.masterdata.entity.MaterialMaster;
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
public interface MaterialMasterRepository extends JpaRepository<MaterialMaster, String> {

    Optional<MaterialMaster> findByCustomerIdAndRecordId(String customerId, String recordId);

    boolean existsByCustomerIdAndCode(String customerId, String code);

    List<MaterialMaster> findByCustomerIdAndCodeOrderByVersionNumberDesc(String customerId, String code);

    Optional<MaterialMaster> findFirstByCustomerIdAndCodeOrderByVersionNumberDesc(String customerId, String code);

    @Query("""
            SELECT m FROM MaterialMaster m WHERE m.customerId = :customerId
              AND (:status IS NULL OR m.status = :status)
              AND (:plantId IS NULL OR m.plantId = :plantId)
              AND (:search IS NULL OR LOWER(m.code) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(m.description) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<MaterialMaster> searchByCustomer(
            @Param("customerId") String customerId,
            @Param("status") MasterDataStatus status,
            @Param("plantId") String plantId,
            @Param("search") String search,
            Pageable pageable);

    @Query("""
            SELECT m FROM MaterialMaster m
            WHERE m.customerId = :customerId AND m.code = :code
              AND m.status = com.automotive.costing.masterdata.entity.MasterDataStatus.ACTIVE
              AND m.effectiveFrom <= :asOf
              AND (m.effectiveTo IS NULL OR m.effectiveTo >= :asOf)
            ORDER BY m.versionNumber DESC
            """)
    List<MaterialMaster> findEffectiveCandidatesForCustomer(
            @Param("customerId") String customerId, @Param("code") String code, @Param("asOf") LocalDate asOf);

    default Optional<MaterialMaster> findEffectiveAsOfForCustomer(String customerId, String code, LocalDate asOf) {
        List<MaterialMaster> list = findEffectiveCandidatesForCustomer(customerId, code, asOf);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }


    boolean existsByCode(String code);

    List<MaterialMaster> findByCodeOrderByVersionNumberDesc(String code);

    Optional<MaterialMaster> findFirstByCodeOrderByVersionNumberDesc(String code);

    @Query("SELECT m FROM MaterialMaster m WHERE " +
           "(:status IS NULL OR m.status = :status) AND " +
           "(:plantId IS NULL OR m.plantId = :plantId) AND " +
           "(:search IS NULL OR LOWER(m.code) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "  OR LOWER(m.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<MaterialMaster> search(@Param("status") MasterDataStatus status,
                                 @Param("plantId") String plantId,
                                 @Param("search") String search,
                                 Pageable pageable);

    boolean existsByCodeAndStatus(String code, MasterDataStatus status);

    long countByStatus(MasterDataStatus status);

    @Query("""
            SELECT m FROM MaterialMaster m
            WHERE m.code = :code
              AND m.status = com.automotive.costing.masterdata.entity.MasterDataStatus.ACTIVE
              AND m.effectiveFrom <= :asOf
              AND (m.effectiveTo IS NULL OR m.effectiveTo >= :asOf)
            ORDER BY m.versionNumber DESC
            """)
    List<MaterialMaster> findEffectiveCandidates(@Param("code") String code, @Param("asOf") LocalDate asOf);

    default Optional<MaterialMaster> findEffectiveAsOf(String code, LocalDate asOf) {
        List<MaterialMaster> list = findEffectiveCandidates(code, asOf);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    long countByCustomerId(String customerId);

    @Query("""
            SELECT COUNT(m) FROM MaterialMaster m
            WHERE m.customerId = :customerId
              AND (m.grade IS NULL OR m.grade = '' OR m.basePrice IS NULL)
            """)
    long countIncompleteByCustomerId(@Param("customerId") String customerId);
}
