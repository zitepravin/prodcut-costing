package com.automotive.costing.masterdata.service;
import com.automotive.costing.common.event.MaterialRatePublishedEvent;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.platform.user.service.AccessScopeService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.DuplicateResourceException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.masterdata.dto.MaterialMasterDtos.CreateRequest;
import com.automotive.costing.masterdata.dto.MaterialMasterDtos.Response;
import com.automotive.costing.masterdata.dto.MaterialMasterDtos.UpdateRequest;
import com.automotive.costing.masterdata.entity.MasterDataStatus;
import com.automotive.costing.masterdata.entity.MaterialMaster;
import com.automotive.costing.masterdata.mapper.MaterialMasterMapper;
import com.automotive.costing.masterdata.mapper.MaterialMasterMapperImpl;
import com.automotive.costing.masterdata.repository.MaterialMasterRepository;
import com.automotive.costing.platform.access.tenant.TenantAccessGuard;
import com.automotive.costing.platform.access.CustomerContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MaterialMasterService, mapped to QA_Test_Case_Tracker.xlsx Module M01
 * (Requirement IDs MDM-01, MDM-02, MDM-05 and the SRS 4.1.5 effective-dating business rule).
 * Test names reference the corresponding TC-#### IDs for traceability (TDD Section 11.3).
 */
@ExtendWith(MockitoExtension.class)
class MaterialMasterServiceTest {

    @Mock
    private MaterialMasterRepository repository;

    @Mock
    private AuditService auditService;

    @Mock
    private TenantAccessGuard tenantAccessGuard;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AccessScopeService accessScopeService;

    private MaterialMasterMapper mapper;

    private MaterialMasterService service;

    @AfterEach
    void tearDown() {
        CustomerContext.clear();
    }

    @BeforeEach
    void setUp() {
        CustomerContext.setCustomerId("test-customer");
        when(tenantAccessGuard.requireCustomerId()).thenReturn("test-customer");
        mapper = new MaterialMasterMapperImpl();
        service = new MaterialMasterService(repository, mapper, auditService, eventPublisher, accessScopeService, tenantAccessGuard);
    }

    private CreateRequest validCreateRequest() {
        return new CreateRequest(
                "MAT-STEEL-001", "Cold rolled steel sheet",
                new BigDecimal("1.850000"), "KG", null,
                LocalDate.of(2026, 1, 1), null,
                "DC04", "DIN", new BigDecimal("7.850000"),
                new BigDecimal("0.250000"), "sheet",
                false, null, null, null
        );
    }

    // Corresponds to TC-0001 (M01 / MDM-01, Positive): unique code creation succeeds.
    @Test
    void create_withUniqueCode_succeedsAndIsAudited() {
        when(repository.existsByCustomerIdAndCode("test-customer", "MAT-STEEL-001")).thenReturn(false);
        when(repository.save(any(MaterialMaster.class))).thenAnswer(inv -> inv.getArgument(0));

        Response result = service.create(validCreateRequest());

        assertThat(result.code()).isEqualTo("MAT-STEEL-001");
        assertThat(result.versionNumber()).isEqualTo(1);
        assertThat(result.status()).isEqualTo(MasterDataStatus.ACTIVE);

        ArgumentCaptor<MaterialMaster> captor = ArgumentCaptor.forClass(MaterialMaster.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getRecordId()).isNotBlank();

        verify(auditService).recordCreate(eq("MaterialMaster"), anyString(), any());
    }

    // Corresponds to TC-0002 (M01 / MDM-01, Negative/Boundary): duplicate code is rejected.
    @Test
    void create_withDuplicateCode_throwsDuplicateResourceException() {
        when(repository.existsByCustomerIdAndCode("test-customer", "MAT-STEEL-001")).thenReturn(true);

        assertThatThrownBy(() -> service.create(validCreateRequest()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(repository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    // Corresponds to TC-0003 (M01 / MDM-02, Positive): a new version is created, not an in-place edit.
    @Test
    void createNewVersion_withValidData_incrementsVersionAndPreservesHistory() {
        MaterialMaster current = existingActiveMaterial();
        when(repository.findFirstByCustomerIdAndCodeOrderByVersionNumberDesc("test-customer", "MAT-STEEL-001"))
                .thenReturn(Optional.of(current));
        when(repository.save(any(MaterialMaster.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateRequest update = new UpdateRequest(
                "Cold rolled steel sheet - revised", new BigDecimal("1.950000"), "KG",
                LocalDate.of(2026, 2, 1), null, null, null, null, null, null,
                null, null, null, null);

        Response result = service.createNewVersion("MAT-STEEL-001", update);

        assertThat(result.versionNumber()).isEqualTo(2);
        assertThat(result.rate()).isEqualByComparingTo("1.950000");
        // Original entity object must never be mutated in place.
        assertThat(current.getVersionNumber()).isEqualTo(1);
        assertThat(current.getRate()).isEqualByComparingTo("1.850000");
        // A ~5.4% variance is within the default 10% threshold, so this should publish
        // straight to ACTIVE and fire the MAT-05 recalculation event.
        assertThat(result.status()).isEqualTo(MasterDataStatus.ACTIVE);

        verify(auditService).recordUpdate(eq("MaterialMaster"), anyString(), any(), any());
        verify(eventPublisher).publishEvent(any(com.automotive.costing.common.event.MaterialRatePublishedEvent.class));
    }

    // MAT-05 / SRS 4.3.5: a rate change exceeding the variance threshold must land in
    // PENDING_APPROVAL (Costing Manager sign-off required) rather than going live
    // immediately, and must NOT fire the MAT-05 recalculation event until it is approved.
    @Test
    void createNewVersion_withRateVarianceOverThreshold_requiresApprovalAndDoesNotPublishEvent() {
        MaterialMaster current = existingActiveMaterial();
        when(repository.findFirstByCustomerIdAndCodeOrderByVersionNumberDesc("test-customer", "MAT-STEEL-001"))
                .thenReturn(Optional.of(current));
        when(repository.save(any(MaterialMaster.class))).thenAnswer(inv -> inv.getArgument(0));

        // 1.85 -> 2.50 is a ~35% increase, well over the 10% default threshold.
        UpdateRequest bigJump = new UpdateRequest(
                null, new BigDecimal("2.500000"), null,
                LocalDate.of(2026, 2, 1), null, null, null, null, null, null,
                null, null, null, null);

        Response result = service.createNewVersion("MAT-STEEL-001", bigJump);

        assertThat(result.status()).isEqualTo(MasterDataStatus.PENDING_APPROVAL);
        verifyNoInteractions(eventPublisher);
    }

    // Corresponds to TC-0004 (M01 / MDM-02, Negative/Boundary): retroactive effective date is rejected.
    @Test
    void createNewVersion_withRetroactiveEffectiveDate_throwsBusinessRuleViolation() {
        MaterialMaster current = existingActiveMaterial();
        when(repository.findFirstByCustomerIdAndCodeOrderByVersionNumberDesc("test-customer", "MAT-STEEL-001"))
                .thenReturn(Optional.of(current));

        UpdateRequest retroactiveUpdate = new UpdateRequest(
                null, new BigDecimal("2.000000"), null,
                LocalDate.of(2025, 12, 1), // before current.effectiveFrom (2026-01-01)
                null, null, null, null, null, null,
                null, null, null, null);

        assertThatThrownBy(() -> service.createNewVersion("MAT-STEEL-001", retroactiveUpdate))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("never retroactively");

        verify(repository, never()).save(any());
    }

    @Test
    void createNewVersion_forUnknownCode_throwsResourceNotFound() {
        when(repository.findFirstByCustomerIdAndCodeOrderByVersionNumberDesc("test-customer", "UNKNOWN"))
                .thenReturn(Optional.empty());

        UpdateRequest update = new UpdateRequest(
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null);

        assertThatThrownBy(() -> service.createNewVersion("UNKNOWN", update))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // Corresponds to TC-0009/TC-0010 (M01 / MDM-05): activation blocked without mandatory attributes.
    @Test
    void create_withoutDensity_resultsInPendingApprovalNotActive() {
        when(repository.existsByCustomerIdAndCode("test-customer", "MAT-STEEL-002")).thenReturn(false);
        when(repository.save(any(MaterialMaster.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateRequest incomplete = new CreateRequest(
                "MAT-STEEL-002", "Missing density", new BigDecimal("1.500000"), "KG", null,
                LocalDate.of(2026, 1, 1), null, "DC04", "DIN",
                null, // density intentionally omitted - MDM-05 must block activation
                null, "sheet",
                false, null, null, null);

        // Bean Validation on the controller layer would normally reject a null density
        // at the HTTP boundary (density is @NotNull on CreateRequest); this test exercises
        // the service-level defense-in-depth check directly by bypassing that annotation
        // via direct service invocation, confirming resolveInitialStatus() never
        // silently activates an incomplete record even if validation were ever bypassed.
        Response result = service.create(incomplete);

        assertThat(result.status()).isEqualTo(MasterDataStatus.PENDING_APPROVAL);
    }

    private MaterialMaster existingActiveMaterial() {
        MaterialMaster m = new MaterialMaster();
        m.setRecordId("11111111-1111-1111-1111-111111111111");
        m.setCode("MAT-STEEL-001");
        m.setDescription("Cold rolled steel sheet");
        m.setRate(new BigDecimal("1.850000"));
        m.setUom("KG");
        m.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        m.setVersionNumber(1);
        m.setStatus(MasterDataStatus.ACTIVE);
        m.setGrade("DC04");
        m.setSpecificationStandard("DIN");
        m.setDensity(new BigDecimal("7.850000"));
        m.setScrapRecoveryRate(new BigDecimal("0.250000"));
        m.setMaterialForm("sheet");
        return m;
    }

    @Test
    void approve_pendingVersion_closesPriorActiveWindow() {
        MaterialMaster v1 = new MaterialMaster();
        v1.setRecordId("r1");
        v1.setCode("MAT-STEEL-001");
        v1.setRate(new BigDecimal("100.000000"));
        v1.setVersionNumber(1);
        v1.setStatus(MasterDataStatus.ACTIVE);
        v1.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        v1.setEffectiveTo(null);

        MaterialMaster v2 = new MaterialMaster();
        v2.setRecordId("r2");
        v2.setCode("MAT-STEEL-001");
        v2.setRate(new BigDecimal("110.000000"));
        v2.setVersionNumber(2);
        v2.setStatus(MasterDataStatus.PENDING_APPROVAL);
        v2.setEffectiveFrom(LocalDate.of(2026, 4, 1));
        v2.setUom("KG");
        v2.setDensity(new BigDecimal("7.85"));

        when(repository.findByCustomerIdAndRecordId("test-customer", "r2")).thenReturn(Optional.of(v2));
        when(repository.findByCustomerIdAndCodeOrderByVersionNumberDesc("test-customer", "MAT-STEEL-001"))
                .thenReturn(java.util.List.of(v2, v1));
        when(repository.save(any(MaterialMaster.class))).thenAnswer(inv -> inv.getArgument(0));

        Response approved = service.approve("r2");
        assertThat(approved.status()).isEqualTo(MasterDataStatus.ACTIVE);
        assertThat(v1.getEffectiveTo()).isEqualTo(LocalDate.of(2026, 3, 31));
        verify(eventPublisher).publishEvent(any(MaterialRatePublishedEvent.class));
    }

    @Test
    void findEffectiveAsOf_returnsHistoricalRate_notLatest() {
        MaterialMaster v1 = new MaterialMaster();
        v1.setRecordId("r1");
        v1.setCode("MAT-STEEL-001");
        v1.setRate(new BigDecimal("100.000000"));
        v1.setVersionNumber(1);
        v1.setStatus(MasterDataStatus.ACTIVE);
        v1.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        v1.setEffectiveTo(LocalDate.of(2026, 3, 31));
        v1.setUom("KG");

        when(repository.findEffectiveAsOfForCustomer("test-customer", "MAT-STEEL-001", LocalDate.of(2026, 2, 15)))
                .thenReturn(Optional.of(v1));

        Response r = service.findEffectiveAsOf("MAT-STEEL-001", LocalDate.of(2026, 2, 15));
        assertThat(r.rate()).isEqualByComparingTo("100.000000");
        assertThat(r.versionNumber()).isEqualTo(1);
    }

    @Test
    void approve_nonPending_rejected() {
        MaterialMaster active = new MaterialMaster();
        active.setRecordId("r1");
        active.setStatus(MasterDataStatus.ACTIVE);
        when(repository.findByCustomerIdAndRecordId("test-customer", "r1")).thenReturn(Optional.of(active));
        assertThatThrownBy(() -> service.approve("r1"))
                .isInstanceOf(BusinessRuleViolationException.class);
    }
}
