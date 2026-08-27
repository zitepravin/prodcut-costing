package com.automotive.costing.fta.repository;
import com.automotive.costing.fta.entity.OriginAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface OriginAssessmentRepository extends JpaRepository<OriginAssessment, String> {
  Optional<OriginAssessment> findByCustomerIdAndAssessmentId(String customerId, String assessmentId);
}
