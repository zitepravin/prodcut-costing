package com.automotive.costing.fta.repository;
import com.automotive.costing.fta.entity.TradeAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface TradeAgreementRepository extends JpaRepository<TradeAgreement, String> {
  Optional<TradeAgreement> findByAgreementCodeAndVersionNo(String code, int versionNo);
  List<TradeAgreement> findByEnabledTrueAndStatus(String status);
}
