package com.automotive.costing.versioning.repository;

import com.automotive.costing.versioning.entity.CostVersionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CostVersionRecordRepository extends JpaRepository<CostVersionRecord, String> {

    List<CostVersionRecord> findByEntityTypeAndEntityIdOrderByVersionNumberDesc(String entityType, String entityId);

    Optional<CostVersionRecord> findFirstByEntityTypeAndEntityIdOrderByVersionNumberDesc(String entityType, String entityId);

    Optional<CostVersionRecord> findByEntityTypeAndEntityIdAndVersionNumber(String entityType, String entityId, int versionNumber);

    Optional<CostVersionRecord> findByEntityTypeAndEntityIdAndProgramBaselineTrue(String entityType, String entityId);

    @Modifying
    @Query("update CostVersionRecord v set v.programBaseline = false where v.entityType = :type and v.entityId = :id and v.programBaseline = true")
    int clearBaseline(@Param("type") String entityType, @Param("id") String entityId);
}
