package com.automotive.costing.mixedcost.repository;

import com.automotive.costing.mixedcost.entity.MixedCostLeg;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MixedCostLegRepository extends JpaRepository<MixedCostLeg, MixedCostLeg.MixedCostLegId> {

    List<MixedCostLeg> findByMixedCostIdOrderByLegSeqAsc(String mixedCostId);

    void deleteByMixedCostId(String mixedCostId);
}
