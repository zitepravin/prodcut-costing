package com.automotive.costing.material.repository;

import com.automotive.costing.material.entity.CommodityIndexValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommodityIndexValueRepository extends JpaRepository<CommodityIndexValue, String> {

    boolean existsByIndexCodeAndEffectiveDate(String indexCode, LocalDate effectiveDate);

    /**
     * MAT-02: the index value effective as of a given date is the most recent publication
     * on or before that date - never a future-dated value, and never simply "the latest
     * value in the table" (which would break reproducibility of historical calculations,
     * TDD Section 6.2).
     */
    Optional<CommodityIndexValue> findFirstByIndexCodeAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
            String indexCode, LocalDate asOfDate);

    List<CommodityIndexValue> findByIndexCodeOrderByEffectiveDateDesc(String indexCode);
}
