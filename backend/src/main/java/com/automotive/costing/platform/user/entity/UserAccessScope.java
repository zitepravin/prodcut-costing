package com.automotive.costing.platform.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * SEC-03: "System shall enforce data segregation by plant, program, and commodity as
 * configured by an administrator." One row = one grant of visibility into a specific
 * plant/program/commodity value for a user. Absence of any rows for a user (of a given
 * {@code scopeType}) means unrestricted access for that dimension - see
 * {@code AccessScopeService}'s Javadoc for the full access-check semantics and why this
 * convention was chosen.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_access_scope")
public class UserAccessScope {

    @Id
    @Column(name = "scope_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String scopeId;

    @Column(name = "user_id", length = 36, nullable = false, columnDefinition = "CHAR(36)")
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", length = 20, nullable = false)
    private ScopeType scopeType;

    @Column(name = "scope_value", length = 100, nullable = false)
    private String scopeValue;

    @Column(name = "created_by", length = 36, columnDefinition = "CHAR(36)")
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
