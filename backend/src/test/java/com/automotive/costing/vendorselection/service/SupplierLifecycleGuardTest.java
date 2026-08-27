package com.automotive.costing.vendorselection.service;

import com.automotive.costing.common.audit.AuditService;
import org.springframework.context.ApplicationEventPublisher;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.vendorselection.entity.SupplierLifecycleStatus;
import com.automotive.costing.vendorselection.entity.SupplierQualificationCurrent;
import com.automotive.costing.vendorselection.repository.SupplierLifecycleOperationBlockRepository;
import com.automotive.costing.vendorselection.repository.SupplierQualificationCurrentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierLifecycleGuardTest {

    @Mock private SupplierQualificationCurrentRepository qualificationCurrentRepository;
    @Mock private SupplierLifecycleOperationBlockRepository blockRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private AuditService auditService;

    @InjectMocks private SupplierLifecycleGuard guard;

    @Test
    void blocked_throwsAndRecords() {
        SupplierQualificationCurrent cur = new SupplierQualificationCurrent();
        cur.setSupplierId("S1");
        cur.setLifecycleStatus(SupplierLifecycleStatus.BLOCKED);
        when(qualificationCurrentRepository.findById("S1")).thenReturn(Optional.of(cur));
        when(blockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> guard.assertNotBlockedOrSuspended("S1", SupplierLifecycleGuard.OP_SCORECARD_EVALUATE))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("BLOCKED");
        verify(blockRepository).save(any());
    }

    @Test
    void suspended_throws() {
        SupplierQualificationCurrent cur = new SupplierQualificationCurrent();
        cur.setSupplierId("S1");
        cur.setLifecycleStatus(SupplierLifecycleStatus.SUSPENDED);
        when(qualificationCurrentRepository.findById("S1")).thenReturn(Optional.of(cur));
        when(blockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> guard.assertNotBlockedOrSuspended("S1", SupplierLifecycleGuard.OP_TCO_CALCULATE))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    void qualified_allows() {
        SupplierQualificationCurrent cur = new SupplierQualificationCurrent();
        cur.setSupplierId("S1");
        cur.setLifecycleStatus(SupplierLifecycleStatus.QUALIFIED);
        when(qualificationCurrentRepository.findById("S1")).thenReturn(Optional.of(cur));

        assertThatCode(() -> guard.assertNotBlockedOrSuspended("S1", SupplierLifecycleGuard.OP_SCORECARD_EVALUATE))
                .doesNotThrowAnyException();
    }

    @Test
    void noQualification_allows() {
        when(qualificationCurrentRepository.findById("S1")).thenReturn(Optional.empty());
        assertThatCode(() -> guard.assertNotBlockedOrSuspended("S1", SupplierLifecycleGuard.OP_TCO_CALCULATE))
                .doesNotThrowAnyException();
    }
}
