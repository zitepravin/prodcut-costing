package com.automotive.costing.fta.repository;
import com.automotive.costing.fta.entity.OriginRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
public interface OriginRuleRepository extends JpaRepository<OriginRule, String> {
  @Query("""
    select r from OriginRule r where r.agreementId = :aid and r.hsCode = :hs
      and r.active = true and r.approvalStatus = 'APPROVED'
      and r.effectiveFrom <= :asOf and (r.effectiveTo is null or r.effectiveTo >= :asOf)
    order by r.versionNo desc
    """)
  List<OriginRule> findEffective(@Param("aid") String agreementId, @Param("hs") String hs, @Param("asOf") LocalDate asOf);
}
