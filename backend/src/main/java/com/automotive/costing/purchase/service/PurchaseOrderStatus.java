package com.automotive.costing.purchase.service;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Purchase order status machine.
 * CLOSED / PARTIALLY_RECEIVED = light operational close (not full ERP GR/IR).
 * Human-only transitions — never AI auto-award.
 */
public final class PurchaseOrderStatus {
    private PurchaseOrderStatus() {}

    public static final String DRAFT = "DRAFT";
    public static final String SUBMITTED = "SUBMITTED";
    public static final String APPROVAL = "APPROVAL";
    public static final String APPROVED = "APPROVED";
    public static final String ISSUED = "ISSUED";
    public static final String PARTIALLY_RECEIVED = "PARTIALLY_RECEIVED";
    public static final String CLOSED = "CLOSED";
    public static final String REJECTED = "REJECTED";
    public static final String REWORK = "REWORK";
    public static final String CANCELLED = "CANCELLED";

    private static final Map<String, Set<String>> ALLOWED = Map.of(
            DRAFT, Set.of(SUBMITTED, CANCELLED),
            SUBMITTED, Set.of(APPROVAL, CANCELLED),
            APPROVAL, Set.of(APPROVED, REJECTED, CANCELLED),
            REJECTED, Set.of(REWORK, CANCELLED),
            REWORK, Set.of(SUBMITTED, CANCELLED),
            APPROVED, Set.of(ISSUED, CANCELLED),
            ISSUED, Set.of(PARTIALLY_RECEIVED, CLOSED, CANCELLED),
            PARTIALLY_RECEIVED, Set.of(CLOSED, CANCELLED),
            CLOSED, Set.of(),
            CANCELLED, Set.of()
    );

    public static void assertTransition(String from, String to) {
        String f = norm(from);
        String t = norm(to);
        Set<String> next = ALLOWED.getOrDefault(f, Set.of());
        if (!next.contains(t)) {
            throw new IllegalArgumentException(
                    "Invalid purchase order status transition " + f + " → " + t);
        }
    }

    public static String norm(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean isEditable(String status) {
        String s = norm(status);
        return DRAFT.equals(s) || REWORK.equals(s);
    }

    public static boolean isTerminal(String status) {
        String s = norm(status);
        return CLOSED.equals(s) || CANCELLED.equals(s);
    }
}
