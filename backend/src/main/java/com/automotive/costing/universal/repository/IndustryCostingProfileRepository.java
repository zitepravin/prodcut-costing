package com.automotive.costing.universal.repository;
import com.automotive.costing.universal.entity.IndustryCostingProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; import java.util.Optional;
public interface IndustryCostingProfileRepository extends JpaRepository<IndustryCostingProfile, String> {
  List<IndustryCostingProfile> findByActiveTrueOrderByProfileCodeAsc();
  Optional<IndustryCostingProfile> findByProfileCode(String profileCode);
}
