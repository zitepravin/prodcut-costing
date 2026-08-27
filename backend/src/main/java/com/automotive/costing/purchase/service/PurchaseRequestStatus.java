package com.automotive.costing.purchase.service;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Wave B controlled transitions for purchase_request.status.
 * RELEASED remains ERP handoff after APPROVED (P3-01 policy).
 */
public final class PurchaseRequestStatus {
    private PurchaseRequestStatus() {}

    public static final String DRAFT = "DRAFT";
    public static final String SUBMITTED = "SUBMITTED";
    public static final String UNDER_REVIEW = "UNDER_REVIEW";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    public static final String CANCELLED = "CANCELLED";
    public static final String RELEASED = "RELEASED";
    /** Wave C: approved PR created an RFQ via existing RFQ service */
    public static final String RFQ_LINKED = "RFQ_LINKED";

    private static final Map<String, Set<String>> ALLOWED = Map.of(
            DRAFT, Set.of(SUBMITTED, CANCELLED),
            SUBMITTED, Set.of(UNDER_REVIEW, APPROVED, REJECTED, CANCELLED),
            UNDER_REVIEW, Set.of(APPROVED, REJECTED, CANCELLED),
            REJECTED, Set.of(DRAFT),
            APPROVED, Set.of(RELEASED, CANCELLED, RFQ_LINKED),
            RFQ_LINKED, Set.of(RELEASED, CANCELLED),
            RELEASED, Set.of(),
            CANCELLED, Set.of()
    );

    public static void assertTransition(String from, String to) {
        String f = norm(from);
        String t = norm(to);
        Set<String> next = ALLOWED.getOrDefault(f, Set.of());
        if (!next.contains(t)) {
            throw new IllegalArgumentException(
                    "Invalid purchase request status transition " + f + " → " + t);
        }
    }

    public static String norm(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
    }
}
