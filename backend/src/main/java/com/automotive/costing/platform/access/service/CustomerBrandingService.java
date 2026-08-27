package com.automotive.costing.platform.access.service;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.common.file.StoredFileEntity;
import com.automotive.costing.common.file.StoredFileService;
import com.automotive.costing.platform.access.CustomerContext;
import com.automotive.costing.platform.access.entity.PlatformCustomer;
import com.automotive.costing.platform.access.repository.PlatformCustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Customer company logo (TC-012). Upload restricted to CUSTOMER_ADMIN via controller.
 * Bytes stored via StoredFile; only file id on platform_customer.
 */
@Service
@RequiredArgsConstructor
public class CustomerBrandingService {

    public static final String ENTITY_TYPE_LOGO = "CUSTOMER_LOGO";
    private static final long MAX_LOGO_BYTES = 2 * 1024 * 1024; // 2 MiB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/webp",
            "image/gif"
    );

    private final PlatformCustomerRepository customerRepository;
    private final StoredFileService storedFileService;

    private String requireCustomerId() {
        String cid = CustomerContext.getCustomerId();
        if (cid == null || cid.isBlank()) {
            throw new BusinessRuleViolationException(
                    "Customer context required (X-Customer-Id with active membership)", "ACCESS-10");
        }
        return cid;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getLogoMetadata() {
        String customerId = requireCustomerId();
        PlatformCustomer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> ResourceNotFoundException.of("PlatformCustomer", customerId));
        String fileId = customer.getLogoFileId();
        if (fileId == null || fileId.isBlank()) {
            return Map.of(
                    "customerId", customerId,
                    "hasLogo", false,
                    "logoFileId", ""
            );
        }
        return Map.of(
                "customerId", customerId,
                "hasLogo", true,
                "logoFileId", fileId
        );
    }

    @Transactional
    public Map<String, Object> uploadLogo(MultipartFile file) throws IOException {
        String customerId = requireCustomerId();
        validateLogoFile(file);

        PlatformCustomer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> ResourceNotFoundException.of("PlatformCustomer", customerId));

        // Store new file first
        StoredFileEntity meta = storedFileService.store(ENTITY_TYPE_LOGO, customerId, file);

        // Best-effort delete previous logo file (do not fail upload if old delete fails)
        String previous = customer.getLogoFileId();
        if (previous != null && !previous.isBlank() && !previous.equals(meta.getFileId())) {
            try {
                storedFileService.delete(previous);
            } catch (Exception ignored) {
                // retain new logo even if old object cannot be deleted
            }
        }

        customer.setLogoFileId(meta.getFileId());
        customerRepository.save(customer);

        return Map.of(
                "customerId", customerId,
                "hasLogo", true,
                "logoFileId", meta.getFileId(),
                "fileName", meta.getFileName(),
                "contentType", meta.getContentType() != null ? meta.getContentType() : "",
                "sizeBytes", meta.getSizeBytes()
        );
    }

    @Transactional
    public Map<String, Object> clearLogo() {
        String customerId = requireCustomerId();
        PlatformCustomer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> ResourceNotFoundException.of("PlatformCustomer", customerId));
        String previous = customer.getLogoFileId();
        customer.setLogoFileId(null);
        customerRepository.save(customer);
        if (previous != null && !previous.isBlank()) {
            try {
                storedFileService.delete(previous);
            } catch (Exception ignored) {
                // cleared association even if blob delete fails
            }
        }
        return Map.of(
                "customerId", customerId,
                "hasLogo", false,
                "logoFileId", ""
        );
    }

    private void validateLogoFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleViolationException("Logo file is required", "LOGO-01");
        }
        if (file.getSize() > MAX_LOGO_BYTES) {
            throw new BusinessRuleViolationException(
                    "Logo file exceeds maximum size of 2 MB", "LOGO-02");
        }
        String ct = file.getContentType();
        if (ct == null || !ALLOWED_CONTENT_TYPES.contains(ct.toLowerCase(Locale.ROOT))) {
            throw new BusinessRuleViolationException(
                    "Logo must be PNG, JPEG, WEBP, or GIF", "LOGO-03");
        }
        String name = file.getOriginalFilename();
        if (name != null) {
            String lower = name.toLowerCase(Locale.ROOT);
            if (!(lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                    || lower.endsWith(".webp") || lower.endsWith(".gif"))) {
                throw new BusinessRuleViolationException(
                        "Logo file extension must be .png, .jpg, .jpeg, .webp, or .gif", "LOGO-04");
            }
        }
    }
}
