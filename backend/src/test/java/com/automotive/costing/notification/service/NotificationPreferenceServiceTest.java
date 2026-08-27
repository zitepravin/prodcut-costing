package com.automotive.costing.notification.service;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.notification.entity.NotificationPreference;
import com.automotive.costing.notification.repository.NotificationPreferenceRepository;
import com.automotive.costing.platform.access.CustomerContext;
import com.automotive.costing.platform.access.service.CentralAccessService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceTest {

    @Mock NotificationPreferenceRepository preferenceRepository;
    @Mock CentralAccessService centralAccessService;
    @InjectMocks NotificationPreferenceService service;

    @AfterEach
    void clear() {
        CustomerContext.clear();
    }

    @Test
    void rejectsUnsubscribedModule() {
        CustomerContext.setCustomerId("cust-1");
        doThrow(new BusinessRuleViolationException("Module PURCHASE is not entitled", "ACCESS-02"))
                .when(centralAccessService).assertModuleEnabled("cust-1", "PURCHASE");

        assertThrows(BusinessRuleViolationException.class, () ->
                service.upsert("user-1", "PURCHASE", "PURCHASE_ORDER_APPROVED", true, true));
        verify(preferenceRepository, never()).save(any());
    }

    @Test
    void upsertWhenEntitled() {
        CustomerContext.setCustomerId("cust-1");
        doNothing().when(centralAccessService).assertModuleEnabled("cust-1", "PURCHASE");
        when(preferenceRepository.findByUserIdAndModuleCodeAndEventType(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(preferenceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        NotificationPreference p = service.upsert("user-1", "PURCHASE", "PURCHASE_ORDER_APPROVED", false, true);
        assertEquals("PURCHASE", p.getModuleCode());
        assertFalse(p.isEmailEnabled());
        assertTrue(p.isInAppEnabled());
    }
}
