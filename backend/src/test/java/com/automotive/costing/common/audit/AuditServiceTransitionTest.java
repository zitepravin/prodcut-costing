package com.automotive.costing.common.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditServiceTransitionTest {

    @Mock private AuditLogRepository auditLogRepository;
    @Mock private ObjectMapper objectMapper;
    @InjectMocks private AuditService auditService;

    @Test
    void recordTransition_persistsActionTransition() throws Exception {
        auditService.recordTransition("SupplierRisk", "r1", "OPEN", "MITIGATING");
        ArgumentCaptor<AuditLogEntry> cap = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(cap.capture());
        assertThat(cap.getValue().getAction()).isEqualTo("TRANSITION");
        assertThat(cap.getValue().getEntityType()).isEqualTo("SupplierRisk");
        assertThat(cap.getValue().getEntityId()).isEqualTo("r1");
    }

    @Test
    void recordVerify_persistsActionVerify() {
        auditService.recordVerify("SupplierQualityMetric", "m1", "PENDING", "VERIFIED");
        ArgumentCaptor<AuditLogEntry> cap = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogRepository).save(cap.capture());
        assertThat(cap.getValue().getAction()).isEqualTo("VERIFY");
        assertThat(cap.getValue().getEntityType()).isEqualTo("SupplierQualityMetric");
    }

    @Test
    void transitionAndVerify_useMandatoryPropagation() throws Exception {
        Method t = AuditService.class.getMethod("recordTransition", String.class, String.class, Object.class, Object.class);
        Method v = AuditService.class.getMethod("recordVerify", String.class, String.class, Object.class, Object.class);
        assertThat(t.getAnnotation(Transactional.class).propagation()).isEqualTo(Propagation.MANDATORY);
        assertThat(v.getAnnotation(Transactional.class).propagation()).isEqualTo(Propagation.MANDATORY);
    }
}
