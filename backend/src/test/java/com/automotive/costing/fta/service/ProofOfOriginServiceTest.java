package com.automotive.costing.fta.service;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.fta.entity.ProofOfOrigin;
import com.automotive.costing.fta.repository.ProofOfOriginRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ProofOfOriginServiceTest {
  @Mock ProofOfOriginRepository repo;
  @Mock TenantAccessGuard guard;
  ProofOfOriginService service;
  AtomicReference<ProofOfOrigin> store = new AtomicReference<>();

  @BeforeEach void setUp() {
    service = new ProofOfOriginService(repo, guard);
    lenient().when(guard.requireCustomerId()).thenReturn("cust-1");
    lenient().when(repo.save(any())).thenAnswer(i -> { store.set(i.getArgument(0)); return i.getArgument(0); });
    lenient().when(repo.findByCustomerIdAndProofId(eq("cust-1"), anyString()))
        .thenAnswer(i -> Optional.ofNullable(store.get()));
  }

  @Test void create_submit_verify() {
    var p = service.create(new ProofOfOriginService.ProofRequest(
        "ag1", "CERTIFICATE", "REF-1", "issuer", "prod", "RVC", "doc", LocalDate.of(2026,1,1), LocalDate.of(2027,1,1)));
    assertEquals("DRAFT", p.getVerificationStatus());
    assertEquals("SUBMITTED", service.transition(p.getProofId(), "SUBMIT").getVerificationStatus());
    assertEquals("VERIFIED", service.transition(p.getProofId(), "VERIFY").getVerificationStatus());
  }

  @Test void reject_expired_verify() {
    var p = service.create(new ProofOfOriginService.ProofRequest(
        "ag1", "CERTIFICATE", "REF-2", null, null, null, null, LocalDate.of(2020,1,1), LocalDate.of(2020,6,1)));
    service.transition(p.getProofId(), "SUBMIT");
    assertThrows(BusinessRuleViolationException.class, () -> service.transition(p.getProofId(), "VERIFY"));
  }

  @Test void wrong_tenant_not_found() {
    when(guard.requireCustomerId()).thenReturn("cust-2");
    when(repo.findByCustomerIdAndProofId(eq("cust-2"), anyString())).thenReturn(Optional.empty());
    assertThrows(BusinessRuleViolationException.class, () -> service.get("x"));
  }
}
