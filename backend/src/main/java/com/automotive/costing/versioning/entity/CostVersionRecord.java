package com.automotive.costing.versioning.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Immutable snapshot of a cost-bearing record (VER-01). Corrections are always a new row.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cost_version_record")
public class CostVersionRecord extends BaseAuditableEntity {

    @Id
    @Column(name = "version_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String versionId;

    @Column(name = "entity_type", length = 50, nullable = false, updatable = false)
    private String entityType;

    @Column(name = "entity_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String entityId;

    @Column(name = "version_number", nullable = false, updatable = false)
    private int versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_trigger", length = 40, nullable = false, updatable = false)
    private ChangeTrigger changeTrigger;

    @Column(name = "change_reference", length = 100, updatable = false)
    private String changeReference;

    @Column(name = "change_comment", length = 1000, updatable = false)
    private String changeComment;

    @Lob
    @Column(name = "snapshot_json", nullable = false, updatable = false, columnDefinition = "LONGTEXT")
    private String snapshotJson;

    @Column(name = "program_baseline", nullable = false)
    private boolean programBaseline;
}
