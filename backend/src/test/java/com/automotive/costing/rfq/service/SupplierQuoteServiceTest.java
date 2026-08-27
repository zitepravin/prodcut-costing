package com.automotive.costing.rfq.service;


import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.versioning.service.CostVersionService;
import com.automotive.costing.platform.user.repository.ConfidentialityWallRepository;
import com.automotive.costing.rfq.dto.RfqDtos.SubmitQuoteRequest;
import com.automotive.costing.rfq.dto.RfqDtos.UpdateQuoteStatusRequest;
import com.automotive.costing.rfq.entity.QuoteStatus;
import com.automotive.costing.rfq.entity.RfqEvent;
import com.automotive.costing.rfq.entity.RfqLine;
import com.automotive.costing.rfq.entity.RfqStatus;
import com.automotive.costing.rfq.entity.SupplierMaster;
import com.automotive.costing.rfq.entity.SupplierQuote;
import com.automotive.costing.rfq.entity.SupplierStatus;
import com.automotive.costing.rfq.repository.RfqLineRepository;
import com.automotive.costing.rfq.repository.SupplierQuoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierQuoteServiceTest {

    @Mock private SupplierQuoteRepository quoteRepository;
    @Mock private RfqLineRepository lineRepository;
    @Mock private RfqService rfqService;
    @Mock private SupplierMasterService supplierService;
    @Mock private ConfidentialityWallRepository wallRepository;
    @Mock private AuditService auditService;
    @Mock private CostVersionService costVersionService;

    
    @Mock private TenantAccessGuard tenantAccessGuard;
    @org.junit.jupiter.api.BeforeEach
    void stubTenantAccessGuard() {
        org.mockito.Mockito.lenient().when(tenantAccessGuard.requireCustomerId()).thenReturn("cust-test-1");
    }
@InjectMocks
    private SupplierQuoteService service;

    private RfqEvent rfq;
    private RfqLine line;
    private SupplierMaster supplier;

    @BeforeEach
    void setUp() {
        rfq = new RfqEvent();
        rfq.setRfqId("rfq-1");
        rfq.setStatus(RfqStatus.OPEN);
        rfq.setCurrencyCode("USD");

        line = new RfqLine();
        line.setRfqLineId("line-1");
        line.setRfqId("rfq-1");
        line.setBomNodeId("bom-1");
        line.setLineNumber(1);

        supplier = new SupplierMaster();
        supplier.setSupplierId("sup-1");
        supplier.setStatus(SupplierStatus.ACTIVE);
    }

    @Test
    void submitPersistsSubmittedQuote() {
        when(rfqService.require("rfq-1")).thenReturn(rfq);
        when(lineRepository.findByRfqLineId("line-1")).thenReturn(Optional.of(line));
        when(supplierService.require("sup-1")).thenReturn(supplier);
        when(quoteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resp = service.submit("rfq-1", new SubmitQuoteRequest(
                "line-1", "sup-1", new BigDecimal("12.500000"), "USD",
                null, 14, null, null, null, null));

        assertThat(resp.unitPrice()).isEqualByComparingTo("12.500000");
        assertThat(resp.status()).isEqualTo(QuoteStatus.SUBMITTED);
        assertThat(resp.bomNodeId()).isEqualTo("bom-1");
        verify(auditService).recordCreate(any(), any(), any());
    }

    @Test
    void acceptSupersedesPriorAcceptedAndReturnsPrice() {
        SupplierQuote existing = new SupplierQuote();
        existing.setQuoteId("old");
        existing.setBomNodeId("bom-1");
        existing.setStatus(QuoteStatus.ACCEPTED);

        SupplierQuote current = new SupplierQuote();
        current.setQuoteId("new");
        current.setRfqId("rfq-1");
        current.setBomNodeId("bom-1");
        current.setUnitPrice(new BigDecimal("10.00"));
        current.setStatus(QuoteStatus.SUBMITTED);
        current.setCurrencyCode("USD");

        when(quoteRepository.findByCustomerIdAndQuoteId("cust-test-1", "new")).thenReturn(Optional.of(current));
        when(quoteRepository.findByBomNodeIdAndStatusOrderByUpdatedAtDesc("bom-1", QuoteStatus.ACCEPTED))
                .thenReturn(List.of(existing));
        when(rfqService.require("rfq-1")).thenReturn(rfq);
        when(quoteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var resp = service.updateStatus("new", new UpdateQuoteStatusRequest(QuoteStatus.ACCEPTED, "NEG-9"));

        assertThat(resp.status()).isEqualTo(QuoteStatus.ACCEPTED);
        assertThat(existing.getStatus()).isEqualTo(QuoteStatus.SUPERSEDED);
        assertThat(rfq.getStatus()).isEqualTo(RfqStatus.AWARDED);

        when(quoteRepository.findFirstByBomNodeIdAndStatusOrderByUpdatedAtDesc("bom-1", QuoteStatus.ACCEPTED))
                .thenReturn(Optional.of(current));
        assertThat(service.acceptedUnitPriceForBomNode("bom-1")).isPresent();
        assertThat(service.acceptedUnitPriceForBomNode("bom-1").orElseThrow())
                .isEqualByComparingTo("10.00");
    }
}
