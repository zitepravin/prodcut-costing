package com.automotive.costing.platform.access.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "customer_role_limit")
public class CustomerRoleLimit {
    @Id
    @Column(name = "role_limit_id", length = 36, nullable = false)
    private String roleLimitId;

    @Column(name = "customer_id", length = 36, nullable = false)
    private String customerId;

    @Column(name = "role_code", length = 50, nullable = false)
    private String roleCode;

    @Column(name = "max_users", nullable = false)
    private int maxUsers;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", length = 36)
    private String createdBy;

    @Column(name = "updated_by", length = 36)
    private String updatedBy;
}
