package com.automotive.costing.fta.repository;
import com.automotive.costing.fta.entity.TariffConcession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
public interface TariffConcessionRepository extends JpaRepository<TariffConcession, String> {
  @Query("""
    select c from TariffConcession c where c.agreementId = :aid and c.hsCode = :hs
      and c.destinationCountry = :dest and c.active = true and c.approvalStatus = 'APPROVED'
      and (c.originCountry is null or c.originCountry = :origin)
      and c.effectiveFrom <= :asOf and (c.effectiveTo is null or c.effectiveTo >= :asOf)
      and c.exclusionFlag = false
    order by c.versionNo desc
    """)
  List<TariffConcession> findEffective(@Param("aid") String agreementId, @Param("hs") String hs,
      @Param("origin") String origin, @Param("dest") String dest, @Param("asOf") LocalDate asOf);
}
