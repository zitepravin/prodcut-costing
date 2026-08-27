package com.automotive.costing.platform.access;

/**
 * Request-scoped customer (tenant) context resolved from {@code X-Customer-Id} header.
 * Optional support-access flag marks READ_ONLY support sessions.
 */
public final class CustomerContext {

    public static final String HEADER = "X-Customer-Id";

    private static final ThreadLocal<String> CUSTOMER = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> SUPPORT_RO = new ThreadLocal<>();

    private CustomerContext() {}

    public static void setCustomerId(String customerId) {
        CUSTOMER.set(customerId);
    }

    public static String getCustomerId() {
        return CUSTOMER.get();
    }

    public static void setSupportReadOnly(boolean v) {
        SUPPORT_RO.set(v);
    }

    public static boolean isSupportReadOnly() {
        return Boolean.TRUE.equals(SUPPORT_RO.get());
    }

    public static void clear() {
        CUSTOMER.remove();
        SUPPORT_RO.remove();
    }
}
