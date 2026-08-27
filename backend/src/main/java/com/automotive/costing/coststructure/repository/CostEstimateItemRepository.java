package com.automotive.costing.coststructure.repository;

import com.automotive.costing.coststructure.entity.CostEstimateItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CostEstimateItemRepository extends JpaRepository<CostEstimateItem, String> {
    List<CostEstimateItem> findByEstimateRefTypeAndEstimateRefIdOrderByLineNoAsc(String type, String refId);
    void deleteByEstimateRefTypeAndEstimateRefId(String type, String refId);
}
