package com.automotive.costing.masterdata.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "uom_master")
public class UomMaster {

    @Id
    @Column(name = "uom_code", length = 10, nullable = false, updatable = false)
    private String uomCode;

    @Column(name = "description", length = 50)
    private String description;
}
