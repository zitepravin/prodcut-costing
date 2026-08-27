package com.automotive.costing.platform.user.entity;

import com.automotive.costing.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SEC-01/SEC-02/SEC-08. Implements Spring Security's UserDetails directly so the
 * JWT filter chain and authentication provider can work against this entity without
 * a separate adapter class. password may be null for SSO-only accounts (TDD 8.1).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "app_user")
public class User extends BaseAuditableEntity implements UserDetails {

    @Id
    @Column(name = "user_id", length = 36, nullable = false, updatable = false, columnDefinition = "CHAR(36)")
    private String userId;

    @Column(name = "username", length = 100, nullable = false, unique = true)
    private String username;

    @Column(name = "email", length = 150, nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "sso_subject", length = 255)
    private String ssoSubject;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Column(name = "employee_id", length = 50)
    private String employeeId;

    @Column(name = "designation", length = 100)
    private String designation;

    @Column(name = "department_id", length = 36, columnDefinition = "CHAR(36)")
    private String departmentId;

    @Column(name = "mobile_number", length = 30)
    private String mobileNumber;



    @Column(name = "status", length = 20, nullable = false)
    private String status; // Active / Inactive

    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled;

    /** SEC-07: TOTP shared secret (Base32). Set on enrollment; only meaningful once mfaEnabled is confirmed true. */
    @Column(name = "mfa_secret", length = 64)
    private String mfaSecret;

    /**
     * SEC-08: incrementing this instantly invalidates every JWT previously issued to this
     * user (see {@code JwtTokenProvider}'s Javadoc for why this - not a separate token
     * blocklist store - is this system's revocation mechanism). Bumped on deactivation
     * and on password change.
     */
    @Column(name = "token_version", nullable = false)
    private int tokenVersion;

    @Column(name = "password_updated_at")
    private java.time.Instant passwordUpdatedAt;

    @Column(name = "last_login_at")
    private java.time.Instant lastLoginAt;

    /** SEC-06-adjacent signal, not itself a lockout mechanism yet - see NEXT_AGENT.md. */
    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getRoleCode()))
                .collect(Collectors.toSet());
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !"Inactive".equalsIgnoreCase(status);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "Active".equalsIgnoreCase(status); // Invited/Inactive cannot authenticate
    }
}
