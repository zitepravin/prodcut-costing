package com.automotive.costing.fta.service;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.fta.entity.ProofOfOrigin;
import com.automotive.costing.fta.repository.ProofOfOriginRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProofOfOriginService {

  private final ProofOfOriginRepository proofRepository;
  private final TenantAccessGuard tenantAccessGuard;

  public record ProofRequest(String agreementId, String proofType, String referenceNumber,
                             String issuer, String producer, String originCriterion,
                             String documentRef, LocalDate issueDate, LocalDate expiryDate) {}

  @Transactional
  public ProofOfOrigin create(ProofRequest req) {
    String customerId = tenantAccessGuard.requireCustomerId();
    if (req.proofType() == null || req.proofType().isBlank())
      throw new BusinessRuleViolationException("proofType required", "PROOF_TYPE");
    ProofOfOrigin p = new ProofOfOrigin();
    p.setProofId(UUID.randomUUID().toString());
    p.setCustomerId(customerId);
    p.setAgreementId(req.agreementId());
    p.setProofType(req.proofType());
    p.setReferenceNumber(req.referenceNumber());
    p.setIssuer(req.issuer());
    p.setProducer(req.producer());
    p.setOriginCriterion(req.originCriterion());
    p.setDocumentRef(req.documentRef());
    p.setIssueDate(req.issueDate());
    p.setExpiryDate(req.expiryDate());
    p.setVerificationStatus("DRAFT");
    Instant now = Instant.now();
    p.setCreatedAt(now);
    p.setUpdatedAt(now);
    return proofRepository.save(p);
  }

  @Transactional
  public ProofOfOrigin transition(String id, String action) {
    String customerId = tenantAccessGuard.requireCustomerId();
    ProofOfOrigin p = proofRepository.findByCustomerIdAndProofId(customerId, id)
        .orElseThrow(() -> new BusinessRuleViolationException("Proof not found", "PROOF_NOT_FOUND"));
    String act = action == null ? "" : action.trim().toUpperCase();
    String st = p.getVerificationStatus();
    switch (act) {
      case "SUBMIT" -> {
        if (!"DRAFT".equals(st)) throw new BusinessRuleViolationException("Only DRAFT can submit", "PROOF_TR");
        p.setVerificationStatus("SUBMITTED");
      }
      case "VERIFY" -> {
        if (!"DRAFT".equals(st) && !"SUBMITTED".equals(st))
          throw new BusinessRuleViolationException("Only DRAFT/SUBMITTED can verify", "PROOF_TR");
        if (p.getExpiryDate() != null && p.getExpiryDate().isBefore(LocalDate.now()))
          throw new BusinessRuleViolationException("Cannot verify expired proof", "PROOF_EXPIRED");
        p.setVerificationStatus("VERIFIED");
      }
      case "REJECT" -> p.setVerificationStatus("REJECTED");
      case "EXPIRE" -> p.setVerificationStatus("EXPIRED");
      default -> throw new BusinessRuleViolationException("Unknown action: " + act, "PROOF_ACTION");
    }
    p.setUpdatedAt(Instant.now());
    return proofRepository.save(p);
  }

  @Transactional(readOnly = true)
  public ProofOfOrigin get(String id) {
    String customerId = tenantAccessGuard.requireCustomerId();
    return proofRepository.findByCustomerIdAndProofId(customerId, id)
        .orElseThrow(() -> new BusinessRuleViolationException("Proof not found", "PROOF_NOT_FOUND"));
  }
}
