package com.automotive.costing.notification.entity;

public final class DeliveryStatus {
    private DeliveryStatus() {}
    public static final String QUEUED = "QUEUED";
    public static final String PROCESSING = "PROCESSING";
    public static final String SENT = "SENT";
    public static final String FAILED = "FAILED";
    public static final String RETRYING = "RETRYING";
}
