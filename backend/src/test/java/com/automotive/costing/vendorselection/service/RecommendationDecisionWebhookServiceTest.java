package com.automotive.costing.vendorselection.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.vendorselection.repository.VendorRecommendationDecisionEventRepository;
import com.automotive.costing.vendorselection.repository.VendorRecommendationEventDeliveryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationDecisionWebhookServiceTest {

    @Mock private VendorRecommendationDecisionEventRepository eventRepository;
    @Mock private VendorRecommendationEventDeliveryRepository deliveryRepository;
    @Mock private VendorRecommendationService recommendationService;
    @Mock private AuditService auditService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private RecommendationDecisionWebhookService service;

    @Test
    void dispatchWhenDisabled_returnsZeroAttempts() {
        ReflectionTestUtils.setField(service, "enabled", false);
        ReflectionTestUtils.setField(service, "webhookUrl", "");
        var summary = service.dispatchUnpublished();
        assertThat(summary.attempted()).isEqualTo(0);
        assertThat(summary.enabled()).isFalse();
    }

    @Test
    void dispatchWhenEnabledButNoPending_returnsZero() {
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "webhookUrl", "http://localhost/hook");
        when(eventRepository.findByPublishedFlagFalseOrderByOccurredAtAsc()).thenReturn(List.of());
        var summary = service.dispatchUnpublished();
        assertThat(summary.attempted()).isEqualTo(0);
        assertThat(summary.enabled()).isTrue();
    }
}
