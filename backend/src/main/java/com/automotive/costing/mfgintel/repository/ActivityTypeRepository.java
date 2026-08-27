package com.automotive.costing.mfgintel.repository;
import com.automotive.costing.mfgintel.entity.ActivityType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ActivityTypeRepository extends JpaRepository<ActivityType, String> {
    Optional<ActivityType> findByActivityCode(String activityCode);
    List<ActivityType> findByActiveTrueOrderByActivityCodeAsc();
}
