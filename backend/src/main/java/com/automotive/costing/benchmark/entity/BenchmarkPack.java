package com.automotive.costing.benchmark.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "benchmark_pack")
public class BenchmarkPack extends BaseAuditableEntity {

    @Id
    @Column(name = "pack_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String packId;

    @Column(name = "pack_code", length = 40, nullable = false)
    private String packCode;

    @Column(name = "pack_name", length = 200, nullable = false)
    private String packName;

    @Column(name = "region_code", length = 20, nullable = false)
    private String regionCode;

    @Column(name = "source_label", length = 200)
    private String sourceLabel;

    @Column(name = "version_label", length = 40, nullable = false)
    private String versionLabel = "1.0";

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private BenchmarkPackStatus status = BenchmarkPackStatus.DRAFT;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "notes", length = 1000)
    private String notes;
}
