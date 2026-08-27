package com.automotive.costing.importcosting.repository;
import com.automotive.costing.importcosting.entity.ImportTaxRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
public interface ImportTaxRuleRepository extends JpaRepository<ImportTaxRule, String> {
  @Query("""
    select r from ImportTaxRule r
    where r.customerId = :customerId and r.active = true and r.approvalStatus = 'APPROVED'
      and (r.destinationCountry is null or r.destinationCountry = :dest)
      and r.effectiveFrom <= :asOf and (r.effectiveTo is null or r.effectiveTo >= :asOf)
    order by r.taxCode, r.versionNo desc
    """)
  List<ImportTaxRule> findEffective(@Param("customerId") String customerId, @Param("dest") String dest, @Param("asOf") LocalDate asOf);
}
