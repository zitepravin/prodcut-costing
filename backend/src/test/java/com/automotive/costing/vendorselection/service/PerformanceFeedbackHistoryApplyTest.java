package com.automotive.costing.vendorselection.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.rfq.service.SupplierMasterService;
import com.automotive.costing.vendorselection.entity.*;
import com.automotive.costing.vendorselection.repository.SupplierDeliveryMetricRepository;
import com.automotive.costing.vendorselection.repository.SupplierPerformanceFeedbackRepository;
import com.automotive.costing.vendorselection.repository.SupplierQualityMetricRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerformanceFeedbackHistoryApplyTest {

    @Mock private SupplierPerformanceFeedbackRepository feedbackRepository;
    @Mock private SupplierQualityMetricRepository qualityRepository;
    @Mock private SupplierDeliveryMetricRepository deliveryRepository;
    @Mock private SupplierMasterService supplierMasterService;
    @Mock private AuditService auditService;

    @InjectMocks private SupplierPerformanceFeedbackService service;

    @Test
    void applyToHistory_createsQualityMetricForPpm() {
        SupplierPerformanceFeedback f = base("QUALITY", "PPM", "12.5");
        when(feedbackRepository.findById("f1")).thenReturn(Optional.of(f));
        when(feedbackRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(qualityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resp = service.applyToHistory("f1");
        assertThat(resp.appliedToHistory()).isTrue();
        assertThat(resp.historyMetricKind()).isEqualTo("QUALITY");
        assertThat(resp.historyMetricId()).isNotBlank();

        ArgumentCaptor<SupplierQualityMetric> cap = ArgumentCaptor.forClass(SupplierQualityMetric.class);
        verify(qualityRepository).save(cap.capture());
        assertThat(cap.getValue().getMetricType()).isEqualTo(QualityMetricType.PPM);
        assertThat(cap.getValue().getMetricValue()).isEqualByComparingTo("12.5");
        assertThat(cap.getValue().getEvidenceStatus()).isEqualTo(EvidenceStatus.VERIFIED);
    }

    @Test
    void applyToHistory_createsDeliveryMetricForOtd() {
        SupplierPerformanceFeedback f = base("DELIVERY", "OTD", "96.5");
        when(feedbackRepository.findById("f1")).thenReturn(Optional.of(f));
        when(feedbackRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(deliveryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resp = service.applyToHistory("f1");
        assertThat(resp.historyMetricKind()).isEqualTo("DELIVERY");
        ArgumentCaptor<SupplierDeliveryMetric> cap = ArgumentCaptor.forClass(SupplierDeliveryMetric.class);
        verify(deliveryRepository).save(cap.capture());
        assertThat(cap.getValue().getOtdPercent()).isEqualByComparingTo("96.5");
    }

    @Test
    void applyToHistory_flagOnlyWhenMetricCodeUnknown() {
        SupplierPerformanceFeedback f = base("QUALITY", "UNKNOWN_CODE", "1");
        when(feedbackRepository.findById("f1")).thenReturn(Optional.of(f));
        when(feedbackRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resp = service.applyToHistory("f1");
        assertThat(resp.appliedToHistory()).isTrue();
        assertThat(resp.historyMetricId()).isNull();
        assertThat(resp.historyMetricKind()).isNull();
    }

    private static SupplierPerformanceFeedback base(String type, String code, String value) {
        SupplierPerformanceFeedback f = new SupplierPerformanceFeedback();
        f.setFeedbackId("f1");
        f.setSupplierId("sup-1");
        f.setFeedbackDate(LocalDate.of(2026, 8, 1));
        f.setFeedbackType(PerformanceFeedbackType.valueOf(type));
        f.setMetricCode(code);
        f.setMetricValue(new BigDecimal(value));
        f.setEvidenceStatus(EvidenceStatus.VERIFIED);
        f.setAppliedToHistory(false);
        return f;
    }
}
