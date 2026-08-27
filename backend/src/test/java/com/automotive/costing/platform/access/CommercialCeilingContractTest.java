package com.automotive.costing.platform.access;

import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.masterdata.repository.PlantMasterRepository;
import com.automotive.costing.platform.access.entity.*;
import com.automotive.costing.platform.access.repository.*;
import com.automotive.costing.platform.access.service.CommercialLicenseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Commercial ceiling contract — automated, no staging host required.
 * Feature OFF / Module OFF / License expired / Quota / Tenant / Customer Admin mutation.
 */
@ExtendWith(MockitoExtension.class)
class CommercialCeilingContractTest {

    @Mock CustomerLicenseRepository licenseRepository;
    @Mock CustomerRoleLimitRepository roleLimitRepository;
    @Mock LicenseHistoryRepository historyRepository;
    @Mock OfflineLicensePackageRepository offlinePackageRepository;
    @Mock CustomerUserMembershipRepository membershipRepository;
    @Mock PlantMasterRepository plantMasterRepository;

    @InjectMocks CommercialLicenseService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "objectMapper", new ObjectMapper());
        ReflectionTestUtils.setField(service, "signingSecret", "test-secret-key-32chars-minimum!!");
    }

    private CustomerLicense lic(String status) {
        return CustomerLicense.builder()
                .licenseId("L1").customerId("c1").status(status)
                .totalUserLimit(10).plantSiteLimit(2)
                .validFrom(LocalDate.now().minusDays(1))
                .validTo(LocalDate.now().plusYears(1))
                .overLimit(false).licenseVersion(1)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build();
    }

    @Test
    void expiredLicense_writeDenied() {
        CustomerLicense l = lic(LicenseStatus.EXPIRED);
        l.setValidTo(LocalDate.now().minusDays(5));
        l.setGraceEndDate(LocalDate.now().minusDays(1));
        when(licenseRepository.findByCustomerId("c1")).thenReturn(Optional.of(l));
        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> service.assertLicenseActive("c1"));
        assertEquals(CommercialLicenseService.LICENSE_EXPIRED, ex.getRuleReference());
    }

    @Test
    void suspendedLicense_writeDenied() {
        when(licenseRepository.findByCustomerId("c1")).thenReturn(Optional.of(lic(LicenseStatus.SUSPENDED)));
        assertEquals(CommercialLicenseService.LICENSE_SUSPENDED,
                assertThrows(BusinessRuleViolationException.class,
                        () -> service.assertLicenseActive("c1")).getRuleReference());
    }

    @Test
    void quotaExceeded_totalUsers() {
        when(licenseRepository.findByCustomerIdForUpdate("c1")).thenReturn(Optional.of(lic(LicenseStatus.ACTIVE)));
        when(membershipRepository.countByCustomerIdAndStatus("c1", MembershipStatus.ACTIVE)).thenReturn(10L);
        assertEquals(CommercialLicenseService.TOTAL_USER_LIMIT_EXCEEDED,
                assertThrows(BusinessRuleViolationException.class,
                        () -> service.assertTotalUserLimit("c1", 1)).getRuleReference());
    }

    @Test
    void quotaExceeded_roleEngineer() {
        CustomerRoleLimit rl = CustomerRoleLimit.builder()
                .roleLimitId("r1").customerId("c1").roleCode("COSTING_ENGINEER")
                .maxUsers(10).createdAt(Instant.now()).updatedAt(Instant.now()).build();
        when(roleLimitRepository.findByCustomerIdAndRoleCodeForUpdate("c1", "COSTING_ENGINEER"))
                .thenReturn(Optional.of(rl));
        when(membershipRepository.countActiveMembersWithRole("c1", "ACTIVE", "COSTING_ENGINEER"))
                .thenReturn(10L);
        assertEquals(CommercialLicenseService.ROLE_USER_LIMIT_EXCEEDED,
                assertThrows(BusinessRuleViolationException.class,
                        () -> service.assertRoleUserLimit("c1", "COSTING_ENGINEER", 1)).getRuleReference());
    }

    @Test
    void quotaExceeded_plant() {
        CustomerLicense l = lic(LicenseStatus.ACTIVE);
        l.setPlantSiteLimit(2);
        when(licenseRepository.findByCustomerIdForUpdate("c1")).thenReturn(Optional.of(l));
        when(plantMasterRepository.countByCustomerId("c1")).thenReturn(2L);
        assertEquals(CommercialLicenseService.PLANT_LIMIT_EXCEEDED,
                assertThrows(BusinessRuleViolationException.class,
                        () -> service.assertPlantLimit("c1", 1)).getRuleReference());
    }

    @Test
    void roleChange_viewerToEngineer_whenFull_denied() {
        CustomerRoleLimit rl = CustomerRoleLimit.builder()
                .roleLimitId("r1").customerId("c1").roleCode("COSTING_ENGINEER")
                .maxUsers(10).createdAt(Instant.now()).updatedAt(Instant.now()).build();
        when(roleLimitRepository.findByCustomerIdAndRoleCodeForUpdate("c1", "COSTING_ENGINEER"))
                .thenReturn(Optional.of(rl));
        when(membershipRepository.countActiveMembersWithRole("c1", "ACTIVE", "COSTING_ENGINEER"))
                .thenReturn(10L);
        assertThrows(BusinessRuleViolationException.class,
                () -> service.assertRoleAssignmentAllowed("c1",
                        Set.of("COSTING_ENGINEER"), Set.of("VIEWER")));
    }

    @Test
    void customerAdmin_cannotAssignSystemAdministrator() {
        assertEquals(CommercialLicenseService.ROLE_NOT_ASSIGNABLE,
                assertThrows(BusinessRuleViolationException.class,
                        () -> service.assertRoleAssignableByCustomerAdmin(
                                Set.of("SYSTEM_ADMINISTRATOR"))).getRuleReference());
    }

    @Test
    void licenseIfLicensed_skipsWhenNoRow() {
        when(licenseRepository.findByCustomerId("c1")).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> service.assertLicenseActiveIfLicensed("c1"));
    }
}
