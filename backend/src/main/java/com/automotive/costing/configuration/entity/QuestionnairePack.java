package com.automotive.costing.configuration.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
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
@Table(name = "questionnaire_pack")
public class QuestionnairePack extends BaseAuditableEntity {
    @Id
    @Column(name = "pack_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String packId;
    @Column(name = "pack_code", length = 50, nullable = false)
    private String packCode;
    @Column(name = "pack_name", length = 200, nullable = false)
    private String packName;
    @Column(name = "purpose", length = 40, nullable = false)
    private String purpose = "EVALUATION";
    @Column(name = "status", length = 20, nullable = false)
    private String status = "DRAFT";
    @Column(name = "version_no", nullable = false)
    private int versionNo = 1;
    @Column(name = "description", length = 1000)
    private String description;
}
