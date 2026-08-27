package com.automotive.costing.platform.access.service;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.common.file.StoredFileEntity;
import com.automotive.costing.common.file.StoredFileService;
import com.automotive.costing.platform.access.CustomerContext;
import com.automotive.costing.platform.access.entity.PlatformCustomer;
import com.automotive.costing.platform.access.repository.PlatformCustomerRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerBrandingServiceTest {

    @Mock PlatformCustomerRepository customerRepository;
    @Mock StoredFileService storedFileService;

    CustomerBrandingService service;

    @BeforeEach
    void setUp() {
        service = new CustomerBrandingService(customerRepository, storedFileService);
        CustomerContext.setCustomerId("cust-a");
    }

    @AfterEach
    void tearDown() {
        CustomerContext.clear();
    }

    @Test
    void getLogo_noLogo_returnsHasLogoFalse() {
        PlatformCustomer c = customer("cust-a", null);
        when(customerRepository.findById("cust-a")).thenReturn(Optional.of(c));

        Map<String, Object> meta = service.getLogoMetadata();
        assertEquals(Boolean.FALSE, meta.get("hasLogo"));
        assertEquals("", meta.get("logoFileId"));
        assertEquals("cust-a", meta.get("customerId"));
    }

    @Test
    void getLogo_withLogo_returnsFileId() {
        PlatformCustomer c = customer("cust-a", "file-1");
        when(customerRepository.findById("cust-a")).thenReturn(Optional.of(c));

        Map<String, Object> meta = service.getLogoMetadata();
        assertEquals(Boolean.TRUE, meta.get("hasLogo"));
        assertEquals("file-1", meta.get("logoFileId"));
    }

    @Test
    void getLogo_requiresCustomerContext() {
        CustomerContext.clear();
        assertThrows(BusinessRuleViolationException.class, () -> service.getLogoMetadata());
        verifyNoInteractions(customerRepository);
    }

    @Test
    void getLogo_unknownCustomer_notFound() {
        when(customerRepository.findById("cust-a")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getLogoMetadata());
    }

    @Test
    void uploadLogo_persistsFileIdOnCustomer() throws Exception {
        PlatformCustomer c = customer("cust-a", null);
        when(customerRepository.findById("cust-a")).thenReturn(Optional.of(c));
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StoredFileEntity stored = new StoredFileEntity();
        stored.setFileId("file-new");
        stored.setFileName("logo.png");
        stored.setContentType("image/png");
        stored.setSizeBytes(12);
        when(storedFileService.store(eq(CustomerBrandingService.ENTITY_TYPE_LOGO), eq("cust-a"), any()))
                .thenReturn(stored);

        MockMultipartFile file = new MockMultipartFile(
                "file", "logo.png", "image/png", new byte[] {1, 2, 3, 4});

        Map<String, Object> result = service.uploadLogo(file);
        assertEquals(Boolean.TRUE, result.get("hasLogo"));
        assertEquals("file-new", result.get("logoFileId"));
        assertEquals("file-new", c.getLogoFileId());
        verify(customerRepository).save(c);
        verify(storedFileService, never()).delete(any());
    }

    @Test
    void uploadLogo_replacesPreviousFile() throws Exception {
        PlatformCustomer c = customer("cust-a", "file-old");
        when(customerRepository.findById("cust-a")).thenReturn(Optional.of(c));
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        StoredFileEntity stored = new StoredFileEntity();
        stored.setFileId("file-new");
        stored.setFileName("logo.jpg");
        stored.setContentType("image/jpeg");
        stored.setSizeBytes(8);
        when(storedFileService.store(any(), any(), any())).thenReturn(stored);

        MockMultipartFile file = new MockMultipartFile(
                "file", "logo.jpg", "image/jpeg", new byte[] {9, 8, 7});

        service.uploadLogo(file);
        assertEquals("file-new", c.getLogoFileId());
        verify(storedFileService).delete("file-old");
    }

    @Test
    void uploadLogo_rejectsEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile("file", "logo.png", "image/png", new byte[0]);
        assertThrows(BusinessRuleViolationException.class, () -> service.uploadLogo(empty));
        verifyNoInteractions(storedFileService);
    }

    @Test
    void uploadLogo_rejectsWrongType() {
        MockMultipartFile pdf = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", new byte[] {1, 2, 3});
        assertThrows(BusinessRuleViolationException.class, () -> service.uploadLogo(pdf));
        verifyNoInteractions(storedFileService);
    }

    @Test
    void clearLogo_nullsAssociationAndDeletesFile() {
        PlatformCustomer c = customer("cust-a", "file-1");
        when(customerRepository.findById("cust-a")).thenReturn(Optional.of(c));
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = service.clearLogo();
        assertEquals(Boolean.FALSE, result.get("hasLogo"));
        assertNull(c.getLogoFileId());
        verify(storedFileService).delete("file-1");
        ArgumentCaptor<PlatformCustomer> cap = ArgumentCaptor.forClass(PlatformCustomer.class);
        verify(customerRepository).save(cap.capture());
        assertNull(cap.getValue().getLogoFileId());
    }

    @Test
    void clearLogo_noExistingLogo_isIdempotent() {
        PlatformCustomer c = customer("cust-a", null);
        when(customerRepository.findById("cust-a")).thenReturn(Optional.of(c));
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = service.clearLogo();
        assertEquals(Boolean.FALSE, result.get("hasLogo"));
        verify(storedFileService, never()).delete(any());
    }

    @Test
    void tenantIsolation_customerADoesNotLoadCustomerB() {
        when(customerRepository.findById("cust-a")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getLogoMetadata());
        verify(customerRepository).findById("cust-a");
        verify(customerRepository, never()).findById("cust-b");
    }

    private static PlatformCustomer customer(String id, String logoFileId) {
        PlatformCustomer c = new PlatformCustomer();
        c.setCustomerId(id);
        c.setCustomerCode("DEFAULT");
        c.setCustomerName("Default");
        c.setLogoFileId(logoFileId);
        return c;
    }
}
