package com.automotive.costing.financial.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "valuation_currency_policy")
public class ValuationCurrencyPolicy {

    @Id
    @Column(name = "policy_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String policyId;

    @Column(name = "policy_code", length = 40, nullable = false)
    private String policyCode;

    @Column(name = "policy_name", length = 200, nullable = false)
    private String policyName;

    @Column(name = "company_currency", length = 3, nullable = false)
    private String companyCurrency;

    @Column(name = "group_currency", length = 3)
    private String groupCurrency;

    @Column(name = "transaction_currency", length = 3)
    private String transactionCurrency;

    @Column(name = "costing_currency", length = 3, nullable = false)
    private String costingCurrency;

    @Column(name = "valuation_date_policy", length = 40, nullable = false)
    private String valuationDatePolicy = "COSTING_DATE";

    @Column(name = "fixed_valuation_date")
    private LocalDate fixedValuationDate;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
