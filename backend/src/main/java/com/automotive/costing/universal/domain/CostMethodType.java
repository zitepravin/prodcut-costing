package com.automotive.costing.universal.domain;
public enum CostMethodType {
    STANDARD, ACTUAL, ESTIMATED, SHOULD, TARGET, JOB, PROCESS, ABC,
    AVERAGE, QUOTATION, PROJECT, LIFECYCLE, HYBRID;
    public String featureCode() { return "COST_METHOD_" + name(); }
}
