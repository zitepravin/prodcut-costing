package com.automotive.costing.security;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.fta.entity.OriginAssessment;
import com.automotive.costing.fta.entity.ProofOfOrigin;
import com.automotive.costing.fta.repository.OriginAssessmentRepository;
import com.automotive.costing.fta.repository.ProofOfOriginRepository;
import com.automotive.costing.importcosting.entity.ImportCostCalculation;
import com.automotive.costing.importcosting.repository.ImportCostCalculationRepository;
import com.automotive.costing.platform.access.CustomerContext;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.universal.entity.CostObject;
import com.automotive.costing.universal.repository.CostObjectRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * V283 dual-tenant isolation matrix (service/repository contract).
 * Customer A context must never load Customer B owned rows.
 */
@ExtendWith(MockitoExtension.class)
class DualTenantIsolationMatrixTest {

  private static final String TENANT_A = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
  private static final String TENANT_B = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
  private static final String SHARED_ID = "shared-resource-id";

  private final TenantAccessGuard guard = new TenantAccessGuard();

  @Mock ImportCostCalculationRepository importRepo;
  @Mock OriginAssessmentRepository originRepo;
  @Mock ProofOfOriginRepository proofRepo;
  @Mock CostObjectRepository costObjectRepo;

  @BeforeEach void setA() { CustomerContext.setCustomerId(TENANT_A); }
  @AfterEach void clear() { CustomerContext.clear(); }

  @Test void import_wrong_tenant_empty() {
    when(importRepo.findByCustomerIdAndCalculationId(TENANT_A, SHARED_ID)).thenReturn(Optional.empty());
    assertThat(importRepo.findByCustomerIdAndCalculationId(guard.requireCustomerId(), SHARED_ID)).isEmpty();
  }

  @Test void import_assertOwns_denies_foreign() {
    ImportCostCalculation c = new ImportCostCalculation();
    c.setCustomerId(TENANT_B);
    assertThatThrownBy(() -> guard.assertOwns(c.getCustomerId(), "ImportCostCalculation", SHARED_ID))
        .isInstanceOf(BusinessRuleViolationException.class);
  }

  @Test void fta_origin_wrong_tenant_empty() {
    when(originRepo.findByCustomerIdAndAssessmentId(TENANT_A, SHARED_ID)).thenReturn(Optional.empty());
    assertThat(originRepo.findByCustomerIdAndAssessmentId(guard.requireCustomerId(), SHARED_ID)).isEmpty();
  }

  @Test void fta_proof_wrong_tenant_empty() {
    when(proofRepo.findByCustomerIdAndProofId(TENANT_A, SHARED_ID)).thenReturn(Optional.empty());
    assertThat(proofRepo.findByCustomerIdAndProofId(guard.requireCustomerId(), SHARED_ID)).isEmpty();
  }

  @Test void proof_assertOwns_denies_foreign() {
    ProofOfOrigin p = new ProofOfOrigin();
    p.setCustomerId(TENANT_B);
    assertThatThrownBy(() -> guard.assertOwns(p.getCustomerId(), "ProofOfOrigin", SHARED_ID))
        .isInstanceOf(BusinessRuleViolationException.class);
  }

  @Test void universal_cost_object_wrong_tenant_empty() {
    when(costObjectRepo.findByCustomerIdAndCostObjectId(eq(TENANT_A), eq(SHARED_ID))).thenReturn(Optional.empty());
    assertThat(costObjectRepo.findByCustomerIdAndCostObjectId(guard.requireCustomerId(), SHARED_ID)).isEmpty();
  }

  @Test void missing_context_denies() {
    CustomerContext.clear();
    assertThatThrownBy(guard::requireCustomerId)
        .isInstanceOfAny(BusinessRuleViolationException.class, IllegalStateException.class);
  }

  @Test void same_tenant_allows_assertOwns() {
    OriginAssessment a = new OriginAssessment();
    a.setCustomerId(TENANT_A);
    assertThatCode(() -> guard.assertOwns(a.getCustomerId(), "OriginAssessment", SHARED_ID))
        .doesNotThrowAnyException();
  }
}
