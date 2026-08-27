package com.automotive.costing.billing.service;

import com.automotive.costing.billing.entity.BillingPlanModule;
import com.automotive.costing.billing.repository.BillingPlanModuleRepository;
import com.automotive.costing.platform.access.dto.AccessDtos.SetEntitlementRequest;
import com.automotive.costing.platform.access.entity.PlatformModule;
import com.automotive.costing.platform.access.repository.PlatformModuleRepository;
import com.automotive.costing.platform.access.service.CentralAccessService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Bridges verified commercial subscription state into Central Access module entitlements.
 * Only this path (and authorized admin CAM APIs) should change commercial entitlements from billing.
 */
@Service
@RequiredArgsConstructor
public class SubscriptionEntitlementService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionEntitlementService.class);

    private final BillingPlanModuleRepository planModuleRepository;
    private final PlatformModuleRepository moduleRepository;
    private final CentralAccessService centralAccessService;

    @Transactional
    public void activatePlanModules(String customerId, String planId, LocalDate effectiveFrom, LocalDate effectiveTo) {
        List<BillingPlanModule> links = planModuleRepository.findByPlanId(planId);
        for (BillingPlanModule link : links) {
            PlatformModule mod = moduleRepository.findById(link.getModuleId()).orElse(null);
            if (mod == null) {
                continue;
            }
            centralAccessService.setEntitlement(new SetEntitlementRequest(
                    customerId,
                    mod.getModuleCode(),
                    true,
                    effectiveFrom,
                    effectiveTo));
            log.info("Activated entitlement customer={} module={} from subscription plan={}",
                    customerId, mod.getModuleCode(), planId);
        }
    }

    @Transactional
    public void deactivatePlanModules(String customerId, String planId) {
        List<BillingPlanModule> links = planModuleRepository.findByPlanId(planId);
        for (BillingPlanModule link : links) {
            PlatformModule mod = moduleRepository.findById(link.getModuleId()).orElse(null);
            if (mod == null) {
                continue;
            }
            centralAccessService.setEntitlement(new SetEntitlementRequest(
                    customerId,
                    mod.getModuleCode(),
                    false,
                    null,
                    null));
            log.info("Deactivated entitlement customer={} module={} after subscription end plan={}",
                    customerId, mod.getModuleCode(), planId);
        }
    }
}
