package com.automotive.costing.fta.domain;
/** Only IN_FORCE may produce preferential duty (date-gated in engine). */
public enum AgreementStatus {
  DRAFT, NEGOTIATING, TOR_SIGNED, CONCLUDED, SIGNED, RATIFIED, IN_FORCE, SUSPENDED, EXPIRED, REPLACED;
  public boolean canPrefer() { return this == IN_FORCE; }
}
