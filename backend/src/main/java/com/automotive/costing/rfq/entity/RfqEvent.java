package com.automotive.costing.rfq.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "rfq_event")
public class RfqEvent extends BaseAuditableEntity {

    @Id
    @Column(name = "rfq_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String rfqId;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "program_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String programId;

    @Column(name = "rfq_number", length = 50, nullable = false)
    private String rfqNumber;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private RfqStatus status;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "currency_code", length = 3, nullable = false, columnDefinition = "CHAR(3)")
    private String currencyCode;

    @Column(name = "notes", length = 1000)
    private String notes;
}
