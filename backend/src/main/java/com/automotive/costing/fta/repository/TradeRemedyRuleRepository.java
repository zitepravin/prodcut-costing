package com.automotive.costing.fta.repository;
import com.automotive.costing.fta.entity.TradeRemedyRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
public interface TradeRemedyRuleRepository extends JpaRepository<TradeRemedyRule, String> {
  @Query("""
    select r from TradeRemedyRule r where r.hsCode = :hs and r.originCountry = :origin
      and r.active = true
      and r.effectiveFrom <= :asOf and (r.effectiveTo is null or r.effectiveTo >= :asOf)
    """)
  List<TradeRemedyRule> findEffective(@Param("hs") String hs, @Param("origin") String origin, @Param("asOf") LocalDate asOf);
}
