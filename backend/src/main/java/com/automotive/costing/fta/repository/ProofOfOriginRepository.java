package com.automotive.costing.fta.repository;
import com.automotive.costing.fta.entity.ProofOfOrigin;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface ProofOfOriginRepository extends JpaRepository<ProofOfOrigin, String> {
  Optional<ProofOfOrigin> findByCustomerIdAndProofId(String customerId, String proofId);
}
