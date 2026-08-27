package com.automotive.costing.platform.access.entity;

import java.util.Locale;
import java.util.Set;

/**
 * Commercial license lifecycle. Business data is never deleted on status change.
 */
public final class LicenseStatus {
    public static final String ACTIVE = "ACTIVE";
    public static final String PAYMENT_DUE = "PAYMENT_DUE";
    public static final String GRACE = "GRACE";
    public static final String SUSPENDED = "SUSPENDED";
    public static final String EXPIRED = "EXPIRED";
    public static final String CANCELLED = "CANCELLED";
    public static final String REACTIVATED = "REACTIVATED";

    /** Statuses that allow mutating business operations (create/approve/lock). */
    public static final Set<String> OPERATIONAL = Set.of(ACTIVE, PAYMENT_DUE, GRACE, REACTIVATED);

    /** Statuses that allow read-only / admin-license visibility. */
    public static final Set<String> READABLE = Set.of(
            ACTIVE, PAYMENT_DUE, GRACE, SUSPENDED, EXPIRED, CANCELLED, REACTIVATED);

    private LicenseStatus() {}

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean isOperational(String status) {
        String s = normalize(status);
        return s != null && OPERATIONAL.contains(s);
    }
}
