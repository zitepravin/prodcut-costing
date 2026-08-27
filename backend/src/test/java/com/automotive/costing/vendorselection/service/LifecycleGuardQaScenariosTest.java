package com.automotive.costing.vendorselection.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.vendorselection.entity.SupplierLifecycleStatus;
import com.automotive.costing.vendorselection.entity.SupplierQualificationCurrent;
import com.automotive.costing.vendorselection.repository.SupplierLifecycleOperationBlockRepository;
import com.automotive.costing.vendorselection.repository.SupplierQualificationCurrentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tester scenarios: BLOCKED/SUSPENDED suppliers cannot score or TCO for award decisions.
 */
@ExtendWith(MockitoExtension.class)
class LifecycleGuardQaScenariosTest {

    @Mock private SupplierQualificationCurrentRepository qualificationCurrentRepository;
    @Mock private SupplierLifecycleOperationBlockRepository blockRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private AuditService auditService;

    @InjectMocks private SupplierLifecycleGuard guard;

    @Test
    @DisplayName("QA: BLOCKED supplier cannot run SCORECARD_EVALUATE")
    void blockedCannotScore() {
        SupplierQualificationCurrent cur = new SupplierQualificationCurrent();
        cur.setSupplierId("S1");
        cur.setLifecycleStatus(SupplierLifecycleStatus.BLOCKED);
        when(qualificationCurrentRepository.findById("S1")).thenReturn(Optional.of(cur));
        when(blockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> guard.assertNotBlockedOrSuspended("S1", SupplierLifecycleGuard.OP_SCORECARD_EVALUATE))
                .isInstanceOf(BusinessRuleViolationException.class);
        verify(blockRepository).save(any());
    }

    @Test
    @DisplayName("QA: QUALIFIED supplier can run TCO_CALCULATE")
    void qualifiedCanTco() {
        SupplierQualificationCurrent cur = new SupplierQualificationCurrent();
        cur.setSupplierId("S1");
        cur.setLifecycleStatus(SupplierLifecycleStatus.QUALIFIED);
        when(qualificationCurrentRepository.findById("S1")).thenReturn(Optional.of(cur));

        assertThatCode(() -> guard.assertNotBlockedOrSuspended("S1", SupplierLifecycleGuard.OP_TCO_CALCULATE))
                .doesNotThrowAnyException();
    }
}
