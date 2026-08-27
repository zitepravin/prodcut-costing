package com.automotive.costing.platform.access.entity;

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
@Table(name = "customer_department")
public class CustomerDepartment extends BaseAuditableEntity {

    @Id
    @Column(name = "department_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String departmentId;

    @Column(name = "customer_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String customerId;

    @Column(name = "department_code", length = 50, nullable = false)
    private String departmentCode;

    @Column(name = "department_name", length = 150, nullable = false)
    private String departmentName;

    @Column(name = "description", length = 500)
    private String description;

    /** ACTIVE / INACTIVE */
    @Column(name = "status", length = 20, nullable = false)
    private String status = "ACTIVE";
}
