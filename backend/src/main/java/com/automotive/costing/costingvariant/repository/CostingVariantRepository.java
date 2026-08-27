package com.automotive.costing.costingvariant.repository;

import com.automotive.costing.costingvariant.entity.CostingVariant;
import com.automotive.costing.costingvariant.entity.CostingVariantStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CostingVariantRepository extends JpaRepository<CostingVariant, String> {
    Optional<CostingVariant> findByVariantCode(String variantCode);
    List<CostingVariant> findAllByOrderByVariantCodeAsc();
    List<CostingVariant> findByStatusOrderByVariantCodeAsc(CostingVariantStatus status);
}
