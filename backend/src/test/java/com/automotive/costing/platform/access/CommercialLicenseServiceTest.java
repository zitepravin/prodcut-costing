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
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommercialLicenseServiceTest {

    @Mock CustomerLicenseRepository licenseRepository;
    @Mock CustomerRoleLimitRepository roleLimitRepository;
    @Mock LicenseHistoryRepository historyRepository;
    @Mock OfflineLicensePackageRepository offlinePackageRepository;
    @Mock CustomerUserMembershipRepository membershipRepository;
    @Mock PlantMasterRepository plantMasterRepository;

    @InjectMocks CommercialLicenseService service;

    private final ObjectMapper om = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "objectMapper", om);
        ReflectionTestUtils.setField(service, "signingSecret", "test-secret-key-32chars-minimum!!");
    }

    private CustomerLicense activeLicense(String customerId, int totalLimit) {
        return CustomerLicense.builder()
                .licenseId("lic-1")
                .customerId(customerId)
                .status(LicenseStatus.ACTIVE)
                .totalUserLimit(totalLimit)
                .plantSiteLimit(5)
                .validFrom(LocalDate.now().minusDays(1))
                .validTo(LocalDate.now().plusYears(1))
                .overLimit(false)
                .licenseVersion(1)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void assertLicenseActive_passesForActive() {
        when(licenseRepository.findByCustomerId("c1")).thenReturn(Optional.of(activeLicense("c1", 50)));
        assertDoesNotThrow(() -> service.assertLicenseActive("c1"));
    }

    @Test
    void assertLicenseActiveIfLicensed_skipsWhenNoLicense() {
        when(licenseRepository.findByCustomerId("c1")).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> service.assertLicenseActiveIfLicensed("c1"));
    }

    @Test
    void assertLicenseActive_rejectsSuspended() {
        CustomerLicense lic = activeLicense("c1", 50);
        lic.setStatus(LicenseStatus.SUSPENDED);
        when(licenseRepository.findByCustomerId("c1")).thenReturn(Optional.of(lic));
        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> service.assertLicenseActive("c1"));
        assertEquals(CommercialLicenseService.LICENSE_SUSPENDED, ex.getRuleReference());
    }

    @Test
    void assertLicenseActive_rejectsExpired() {
        CustomerLicense lic = activeLicense("c1", 50);
        lic.setStatus(LicenseStatus.EXPIRED);
        lic.setValidTo(LocalDate.now().minusDays(10));
        lic.setGraceEndDate(LocalDate.now().minusDays(1));
        when(licenseRepository.findByCustomerId("c1")).thenReturn(Optional.of(lic));
        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> service.assertLicenseActive("c1"));
        assertEquals(CommercialLicenseService.LICENSE_EXPIRED, ex.getRuleReference());
    }

    @Test
    void assertTotalUserLimit_rejectsWhenFull() {
        when(licenseRepository.findByCustomerIdForUpdate("c1"))
                .thenReturn(Optional.of(activeLicense("c1", 10)));
        when(membershipRepository.countByCustomerIdAndStatus("c1", MembershipStatus.ACTIVE))
                .thenReturn(10L);
        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> service.assertTotalUserLimit("c1", 1));
        assertEquals(CommercialLicenseService.TOTAL_USER_LIMIT_EXCEEDED, ex.getRuleReference());
    }

    @Test
    void assertRoleUserLimit_rejectsEleventhEngineer() {
        CustomerRoleLimit rl = CustomerRoleLimit.builder()
                .roleLimitId("rl-1")
                .customerId("c1")
                .roleCode("COSTING_ENGINEER")
                .maxUsers(10)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(roleLimitRepository.findByCustomerIdAndRoleCodeForUpdate("c1", "COSTING_ENGINEER"))
                .thenReturn(Optional.of(rl));
        when(membershipRepository.countActiveMembersWithRole("c1", "ACTIVE", "COSTING_ENGINEER"))
                .thenReturn(10L);
        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> service.assertRoleUserLimit("c1", "COSTING_ENGINEER", 1));
        assertEquals(CommercialLicenseService.ROLE_USER_LIMIT_EXCEEDED, ex.getRuleReference());
    }

    @Test
    void assertPlantLimit_rejectsWhenFull() {
        CustomerLicense lic = activeLicense("c1", 50);
        lic.setPlantSiteLimit(3);
        when(licenseRepository.findByCustomerIdForUpdate("c1")).thenReturn(Optional.of(lic));
        when(plantMasterRepository.countByCustomerId("c1")).thenReturn(3L);
        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> service.assertPlantLimit("c1", 1));
        assertEquals(CommercialLicenseService.PLANT_LIMIT_EXCEEDED, ex.getRuleReference());
    }

    @Test
    void assertRoleAssignmentAllowed_viewerToEngineer_whenFull_rejects() {
        CustomerRoleLimit rl = CustomerRoleLimit.builder()
                .roleLimitId("rl-1")
                .customerId("c1")
                .roleCode("COSTING_ENGINEER")
                .maxUsers(10)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(roleLimitRepository.findByCustomerIdAndRoleCodeForUpdate("c1", "COSTING_ENGINEER"))
                .thenReturn(Optional.of(rl));
        when(membershipRepository.countActiveMembersWithRole("c1", "ACTIVE", "COSTING_ENGINEER"))
                .thenReturn(10L);
        assertThrows(BusinessRuleViolationException.class,
                () -> service.assertRoleAssignmentAllowed(
                        "c1", Set.of("COSTING_ENGINEER"), Set.of("VIEWER")));
    }

    @Test
    void assertRoleAssignableByCustomerAdmin_rejectsSystemAdmin() {
        assertThrows(BusinessRuleViolationException.class,
                () -> service.assertRoleAssignableByCustomerAdmin(Set.of("SYSTEM_ADMINISTRATOR")));
    }

    @Test
    void offlineImport_rejectsBadSignature() {
        assertThrows(BusinessRuleViolationException.class,
                () -> service.importOfflinePackage("c1", "{\"customerId\":\"c1\"}", "deadbeef", "actor"));
    }

    @Test
    void offlineImport_rejectsWrongCustomer() {
        String payload = "{\"customerId\":\"other\",\"status\":\"ACTIVE\",\"totalUserLimit\":5}";
        String sig = service.signPayload(payload);
        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> service.importOfflinePackage("c1", payload, sig, "actor"));
        assertEquals(CommercialLicenseService.LICENSE_PACKAGE_INVALID, ex.getRuleReference());
    }

    @Test
    void offlineImport_rejectsExpiredPackage() {
        String payload = "{\"customerId\":\"c1\",\"status\":\"ACTIVE\",\"totalUserLimit\":5,"
                + "\"validFrom\":\"2020-01-01\",\"validTo\":\"2020-12-31\"}";
        String sig = service.signPayload(payload);
        BusinessRuleViolationException ex = assertThrows(BusinessRuleViolationException.class,
                () -> service.importOfflinePackage("c1", payload, sig, "actor"));
        assertEquals(CommercialLicenseService.LICENSE_EXPIRED, ex.getRuleReference());
    }

    @Test
    void offlineImport_acceptsValidSignatureAndUpserts() {
        String payload = "{\"customerId\":\"c1\",\"status\":\"ACTIVE\",\"totalUserLimit\":25,"
                + "\"plantSiteLimit\":3,\"planCode\":\"STD\",\"packageVersion\":1,"
                + "\"validFrom\":\"" + LocalDate.now() + "\","
                + "\"roleLimits\":[{\"roleCode\":\"COSTING_ENGINEER\",\"maxUsers\":10}]}";
        String sig = service.signPayload(payload);
        when(licenseRepository.findByCustomerId("c1")).thenReturn(Optional.empty());
        when(licenseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(roleLimitRepository.findByCustomerIdAndRoleCode(eq("c1"), anyString()))
                .thenReturn(Optional.empty());
        when(roleLimitRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(offlinePackageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(historyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(membershipRepository.countByCustomerIdAndStatus(eq("c1"), any())).thenReturn(0L);

        CustomerLicense lic = service.importOfflinePackage("c1", payload, sig, "platform");
        assertEquals("c1", lic.getCustomerId());
        assertEquals(25, lic.getTotalUserLimit());
        assertEquals(LicenseStatus.ACTIVE, lic.getStatus());
        verify(offlinePackageRepository).save(any());
        verify(roleLimitRepository, atLeastOnce()).save(any());
    }

    @Test
    void usageSnapshot_reportsRoleAndPlantUsage() {
        when(licenseRepository.findByCustomerId("c1")).thenReturn(Optional.of(activeLicense("c1", 50)));
        when(membershipRepository.countByCustomerIdAndStatus("c1", MembershipStatus.ACTIVE)).thenReturn(8L);
        when(plantMasterRepository.countByCustomerId("c1")).thenReturn(2L);
        CustomerRoleLimit rl = CustomerRoleLimit.builder()
                .roleLimitId("rl-1").customerId("c1").roleCode("COSTING_ENGINEER")
                .maxUsers(10).createdAt(Instant.now()).updatedAt(Instant.now()).build();
        when(roleLimitRepository.findByCustomerId("c1")).thenReturn(java.util.List.of(rl));
        when(membershipRepository.countActiveMembersWithRole("c1", "ACTIVE", "COSTING_ENGINEER"))
                .thenReturn(8L);
        var snap = service.usageSnapshot("c1");
        assertEquals(8L, snap.get("activeUsers"));
        assertEquals(50, snap.get("totalUserLimit"));
        assertEquals(2L, snap.get("plantSiteUsed"));
    }

    /**
     * Concurrent role assignments at limit=10 with current=10 must all fail (cannot produce 11).
     * Simulates sequential lock acquisition: each thread sees count=10 after lock.
     */
    @Test
    void concurrentRoleAssignment_atLimit_allRejected() throws Exception {
        CustomerRoleLimit rl = CustomerRoleLimit.builder()
                .roleLimitId("rl-1")
                .customerId("c1")
                .roleCode("COSTING_ENGINEER")
                .maxUsers(10)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(roleLimitRepository.findByCustomerIdAndRoleCodeForUpdate("c1", "COSTING_ENGINEER"))
                .thenReturn(Optional.of(rl));
        when(membershipRepository.countActiveMembersWithRole("c1", "ACTIVE", "COSTING_ENGINEER"))
                .thenReturn(10L);

        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        AtomicInteger rejected = new AtomicInteger();
        AtomicInteger allowed = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    service.assertRoleUserLimit("c1", "COSTING_ENGINEER", 1);
                    allowed.incrementAndGet();
                } catch (BusinessRuleViolationException e) {
                    rejected.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(15, TimeUnit.SECONDS));
        pool.shutdown();
        assertEquals(0, allowed.get(), "no assignment may succeed when already at limit");
        assertEquals(threads, rejected.get());
    }
}
