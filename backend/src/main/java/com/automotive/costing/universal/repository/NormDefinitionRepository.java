package com.automotive.costing.universal.repository;

import com.automotive.costing.universal.entity.NormDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface NormDefinitionRepository extends JpaRepository<NormDefinition, String> {

    List<NormDefinition> findByCustomerIdOrderByNormCodeAscVersionNoDesc(String customerId);

    Optional<NormDefinition> findByCustomerIdAndNormId(String customerId, String normId);

    @Query("SELECT COALESCE(MAX(n.versionNo), 0) FROM NormDefinition n WHERE n.customerId = :customerId AND n.normCode = :normCode")
    int findMaxVersionNo(@Param("customerId") String customerId, @Param("normCode") String normCode);

    @Query("""
        SELECT n FROM NormDefinition n
        WHERE n.customerId = :customerId
          AND n.normCode = :normCode
          AND n.active = true
          AND n.approvalStatus = 'APPROVED'
          AND n.effectiveFrom <= :asOf
          AND (n.effectiveTo IS NULL OR n.effectiveTo >= :asOf)
        ORDER BY n.versionNo DESC
        """)
    List<NormDefinition> findEffective(
            @Param("customerId") String customerId,
            @Param("normCode") String normCode,
            @Param("asOf") LocalDate asOf);
}
