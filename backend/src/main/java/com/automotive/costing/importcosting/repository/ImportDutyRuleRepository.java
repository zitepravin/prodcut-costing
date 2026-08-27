package com.automotive.costing.importcosting.repository;
import com.automotive.costing.importcosting.entity.ImportDutyRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
public interface ImportDutyRuleRepository extends JpaRepository<ImportDutyRule, String> {
  @Query("""
    select r from ImportDutyRule r
    where r.customerId = :customerId and r.hsCode = :hs and r.originCountry = :origin
      and r.destinationCountry = :dest and r.active = true and r.approvalStatus = 'APPROVED'
      and r.effectiveFrom <= :asOf and (r.effectiveTo is null or r.effectiveTo >= :asOf)
    order by r.versionNo desc
    """)
  List<ImportDutyRule> findEffective(@Param("customerId") String customerId, @Param("hs") String hs,
      @Param("origin") String origin, @Param("dest") String dest, @Param("asOf") LocalDate asOf);
}
