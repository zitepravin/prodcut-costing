package com.automotive.costing.billing.service;

import com.automotive.costing.billing.gateway.PaymentGateway;
import com.automotive.costing.billing.gateway.PaymentGatewayRegistry;

import com.automotive.costing.billing.entity.BillingChargeback;
import com.automotive.costing.billing.entity.BillingPayment;
import com.automotive.costing.billing.entity.BillingRefund;
import com.automotive.costing.billing.entity.CustomerSubscription;
import com.automotive.costing.billing.repository.BillingChargebackRepository;
import com.automotive.costing.billing.repository.BillingPaymentRepository;
import com.automotive.costing.billing.repository.BillingRefundRepository;
import com.automotive.costing.billing.repository.CustomerSubscriptionRepository;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Refund / chargeback recording and commercial access policy execution.
 * FE payment success never activates access; only verified PAID webhooks do.
 * Refunds/chargebacks may revoke or schedule end of CAM entitlements per policy.
 */
@Service
@RequiredArgsConstructor
public class BillingAccessPolicyService {

    private static final Logger log = LoggerFactory.getLogger(BillingAccessPolicyService.class);

    public static final String POLICY_KEEP_UNTIL_PERIOD_END = "KEEP_UNTIL_PERIOD_END";
    public static final String POLICY_IMMEDIATE_REVOKE = "IMMEDIATE_REVOKE";
    public static final String POLICY_GRACE_PERIOD = "GRACE_PERIOD";

    public static final String REF_COMPLETED = "COMPLETED";
    public static final String REF_PENDING = "PENDING";
    public static final String CB_OPEN = "OPEN";
    public static final String CB_LOST = "LOST";
    public static final String CB_WON = "WON";

    private final BillingPaymentRepository paymentRepository;
    private final PaymentGatewayRegistry gatewayRegistry;
    private final BillingRefundRepository refundRepository;
    private final BillingChargebackRepository chargebackRepository;
    private final CustomerSubscriptionRepository subscriptionRepository;
    private final SubscriptionEntitlementService entitlementService;

    @Transactional
    public BillingRefund recordRefund(String paymentId, BigDecimal amount, String reason, String accessPolicy) {
        BillingPayment pay = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
        if (!BillingSubscriptionService.PAY_PAID.equals(pay.getStatus())) {
            throw new BusinessRuleViolationException("Refund only allowed on PAID payments", "BILLING-REF-01");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleViolationException("Refund amount must be positive", "BILLING-REF-02");
        }
        BigDecimal scaled = amount.setScale(4, RoundingMode.HALF_UP);
        if (scaled.compareTo(pay.getAmount()) > 0) {
            throw new BusinessRuleViolationException("Refund amount exceeds payment", "BILLING-REF-03");
        }
        String policy = normalizePolicy(accessPolicy);

        try {
            PaymentGateway gw = gatewayRegistry.require(pay.getGatewayCode());
            var pr = gw.createRefund(new PaymentGateway.RefundRequest(
                    pay.getGatewayPaymentRef(), pay.getGatewayPaymentRef(), scaled, pay.getCurrencyCode(), reason));
            if (pr.isPresent() && !pr.get().success() && !"CONFIG".equals(pr.get().status())) {
                throw new BusinessRuleViolationException("Provider refund failed: " + pr.get().failureReason(), "BILLING-REF-GW");
            }
        } catch (BusinessRuleViolationException e) {
            if ("BILLING-REF-GW".equals(e.getRuleReference())) throw e;
            // BILLING-01 unknown gateway / offline bookkeeping-only
        }
        BillingRefund refund = new BillingRefund();
        refund.setRefundId(UUID.randomUUID().toString());
        refund.setPaymentId(paymentId);
        refund.setAmount(scaled);
        refund.setCurrencyCode(pay.getCurrencyCode());
        refund.setReason(reason);
        refund.setStatus(REF_COMPLETED);
        refund.setAccessPolicy(policy);
        refund.setRefundedAt(Instant.now());
        refundRepository.save(refund);

        applyAccessPolicy(pay, policy, "REFUND");
        log.info("Refund recorded payment={} amount={} policy={}", paymentId, scaled, policy);
        return refund;
    }

    @Transactional
    public BillingChargeback recordChargeback(String paymentId, BigDecimal amount, String gatewayCbRef, String accessPolicy) {
        BillingPayment pay = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));
        if (!BillingSubscriptionService.PAY_PAID.equals(pay.getStatus())) {
            throw new BusinessRuleViolationException("Chargeback only on PAID payments", "BILLING-CB-01");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleViolationException("Chargeback amount must be positive", "BILLING-CB-02");
        }
        String policy = normalizePolicy(accessPolicy);

        BillingChargeback cb = new BillingChargeback();
        cb.setChargebackId(UUID.randomUUID().toString());
        cb.setPaymentId(paymentId);
        cb.setAmount(amount.setScale(4, RoundingMode.HALF_UP));
        cb.setCurrencyCode(pay.getCurrencyCode());
        cb.setGatewayCbRef(gatewayCbRef);
        cb.setStatus(CB_OPEN);
        cb.setAccessAction(policy);
        chargebackRepository.save(cb);

        // Chargebacks default to immediate commercial risk — apply policy now
        applyAccessPolicy(pay, policy, "CHARGEBACK");
        log.info("Chargeback recorded payment={} amount={} policy={}", paymentId, amount, policy);
        return cb;
    }

    public List<BillingRefund> listRefunds() {
        return refundRepository.findAll();
    }

    public List<BillingChargeback> listChargebacks() {
        return chargebackRepository.findAll();
    }

    /**
     * Lightweight revenue snapshot for admin reports (PAID payments minus completed refunds).
     */
    public RevenueSnapshot revenueSnapshot() {
        List<BillingPayment> paid = paymentRepository.findByStatus(BillingSubscriptionService.PAY_PAID);
        BigDecimal paidTotal = paid.stream()
                .map(BillingPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
        List<BillingRefund> refunds = refundRepository.findAll().stream()
                .filter(r -> REF_COMPLETED.equals(r.getStatus()))
                .toList();
        BigDecimal refundTotal = refunds.stream()
                .map(BillingRefund::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(4, RoundingMode.HALF_UP);
        long openCb = chargebackRepository.findAll().stream()
                .filter(c -> CB_OPEN.equals(c.getStatus()))
                .count();
        return new RevenueSnapshot(
                paidTotal,
                refundTotal,
                paidTotal.subtract(refundTotal).setScale(4, RoundingMode.HALF_UP),
                paid.size(),
                refunds.size(),
                openCb);
    }

    private void applyAccessPolicy(BillingPayment pay, String policy, String source) {
        CustomerSubscription sub = subscriptionRepository.findById(pay.getSubscriptionId()).orElse(null);
        if (sub == null) {
            log.warn("{} policy skipped — subscription missing for payment {}", source, pay.getPaymentId());
            return;
        }
        switch (policy) {
            case POLICY_IMMEDIATE_REVOKE -> {
                entitlementService.deactivatePlanModules(sub.getCustomerId(), sub.getPlanId());
                sub.setStatus(BillingSubscriptionService.ST_SUSPENDED);
                subscriptionRepository.save(sub);
            }
            case POLICY_GRACE_PERIOD -> {
                LocalDate graceEnd = LocalDate.now().plusDays(7);
                sub.setStatus(BillingSubscriptionService.ST_GRACE);
                sub.setGraceEndDate(graceEnd);
                subscriptionRepository.save(sub);
                entitlementService.activatePlanModules(
                        sub.getCustomerId(), sub.getPlanId(), LocalDate.now(), graceEnd);
            }
            case POLICY_KEEP_UNTIL_PERIOD_END -> {
                // Entitlements remain until existing end_date; do not extend
                log.info("{} KEEP_UNTIL_PERIOD_END subscription={} end={}", source, sub.getSubscriptionId(), sub.getEndDate());
            }
            default -> throw new BusinessRuleViolationException("Unknown access policy: " + policy, "BILLING-POL-01");
        }
    }

    private static String normalizePolicy(String accessPolicy) {
        if (accessPolicy == null || accessPolicy.isBlank()) {
            return POLICY_KEEP_UNTIL_PERIOD_END;
        }
        String p = accessPolicy.trim().toUpperCase();
        if (!POLICY_KEEP_UNTIL_PERIOD_END.equals(p)
                && !POLICY_IMMEDIATE_REVOKE.equals(p)
                && !POLICY_GRACE_PERIOD.equals(p)) {
            throw new BusinessRuleViolationException("Invalid access policy: " + accessPolicy, "BILLING-POL-01");
        }
        return p;
    }

    public record RevenueSnapshot(
            BigDecimal paidTotal,
            BigDecimal refundTotal,
            BigDecimal netTotal,
            int paidPaymentCount,
            int completedRefundCount,
            long openChargebackCount) {}
}
