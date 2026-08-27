package com.automotive.costing.platform.user.repository;

import com.automotive.costing.platform.user.entity.ConfidentialityWall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConfidentialityWallRepository extends JpaRepository<ConfidentialityWall, String> {

    List<ConfidentialityWall> findByRfqEventReference(String rfqEventReference);
}
