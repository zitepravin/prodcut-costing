package com.automotive.costing.vendorselection.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "vendor_scorecard_template")
public class VendorScorecardTemplate extends BaseAuditableEntity {
    @Id
    @Column(name = "template_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String templateId;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;
    @Column(name = "template_code", length = 50, nullable = false)
    private String templateCode;
    @Column(name = "template_name", length = 200, nullable = false)
    private String templateName;
    @Column(name = "description", length = 1000)
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private ScorecardTemplateStatus status = ScorecardTemplateStatus.ACTIVE;

    @Column(name = "parent_template_id", length = 36, columnDefinition = "CHAR(36)")
    private String parentTemplateId;

    @Column(name = "lineage_root_id", length = 36, columnDefinition = "CHAR(36)")
    private String lineageRootId;

    @Column(name = "version_no", nullable = false)
    private int versionNo = 1;

    @Column(name = "version_label", length = 100)
    private String versionLabel;

    /** P3-07a §11: system default when tenant has no other selection. */
    @Column(name = "is_system_default", nullable = false)
    private boolean systemDefault;
}
