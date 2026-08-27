package com.automotive.costing.importcosting.domain;
public enum ImportLifecycleStatus {
  DRAFT, SUBMITTED, APPROVED, LOCKED, SUPERSEDED;
  public boolean isMutable() {
    return this == DRAFT || this == SUBMITTED;
  }
}
