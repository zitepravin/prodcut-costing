package com.automotive.costing.emissions.repository;

import com.automotive.costing.emissions.entity.EmissionFactor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmissionFactorRepository extends JpaRepository<EmissionFactor, String> {
    List<EmissionFactor> findAllByOrderByFactorCodeAscVersionNoAsc();
    Optional<EmissionFactor> findByFactorCodeAndVersionNo(String factorCode, int versionNo);

    List<EmissionFactor> findByCategoryAndStatusOrderByFactorCodeAsc(String category, String status);

    Optional<EmissionFactor> findFirstByCategoryAndStatusOrderByVersionNoDesc(String category, String status);
    boolean existsByFactorCodeIgnoreCaseAndVersionNo(String factorCode, int versionNo);
}
