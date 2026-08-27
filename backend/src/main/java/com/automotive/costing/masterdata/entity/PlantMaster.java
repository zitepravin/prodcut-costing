package com.automotive.costing.masterdata.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "plant_master")
public class PlantMaster {

    @Id
    @Column(name = "plant_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String plantId;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "plant_code", length = 20, nullable = false, unique = true)
    private String plantCode;

    @Column(name = "country", length = 2)
    private String country;

    @Column(name = "statutory_duty_notes", columnDefinition = "TEXT")
    private String statutoryDutyNotes;

    @Column(name = "freight_zone", length = 50)
    private String freightZone;
}
