package com.automotive.costing.platform.user.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.BusinessRuleViolationException;
import com.automotive.costing.common.exception.DuplicateResourceException;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.platform.user.dto.AdminDtos.CreateUserRequest;
import com.automotive.costing.platform.user.dto.AdminDtos.UserResponse;
import com.automotive.costing.platform.user.entity.Role;
import com.automotive.costing.platform.user.entity.User;
import com.automotive.costing.platform.user.repository.RoleRepository;
import com.automotive.costing.platform.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditService auditService;

    private UserAdminService service;

    @BeforeEach
    void setUp() {
        service = new UserAdminService(userRepository, roleRepository, passwordEncoder,
                new PasswordPolicyValidator(), auditService);
    }

    private Role costingEngineerRole() {
        return Role.builder().roleId("role-1").roleCode("COSTING_ENGINEER").systemRole(true).build();
    }

    // SEC-01 (Negative/Boundary): a duplicate username is rejected before touching password policy.
    @Test
    void createUser_withDuplicateUsername_throwsDuplicateResourceException() {
        when(userRepository.existsByUsername("jsmith")).thenReturn(true);

        CreateUserRequest request = new CreateUserRequest("jsmith", "j@example.com",
                "ValidPass1!", Set.of("COSTING_ENGINEER"));

        assertThatThrownBy(() -> service.createUser(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(roleRepository, never()).findByRoleCode(any());
    }

    // SEC-06 (Negative/Boundary): a weak password is rejected before any persistence occurs.
    @Test
    void createUser_withWeakPassword_throwsBusinessRuleViolationAndDoesNotPersist() {
        when(userRepository.existsByUsername("jsmith")).thenReturn(false);
        when(userRepository.existsByEmail("j@example.com")).thenReturn(false);

        CreateUserRequest request = new CreateUserRequest("jsmith", "j@example.com",
                "weak", Set.of("COSTING_ENGINEER"));

        assertThatThrownBy(() -> service.createUser(request))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("SEC-06-password-policy");

        verify(userRepository, never()).save(any());
    }

    // SEC-01 (Negative/Boundary): an unknown role code is rejected, not silently skipped.
    @Test
    void createUser_withUnknownRoleCode_throwsResourceNotFound() {
        when(userRepository.existsByUsername("jsmith")).thenReturn(false);
        when(userRepository.existsByEmail("j@example.com")).thenReturn(false);
        when(roleRepository.findByRoleCode("NOT_A_REAL_ROLE")).thenReturn(Optional.empty());

        CreateUserRequest request = new CreateUserRequest("jsmith", "j@example.com",
                "ValidPass1!", Set.of("NOT_A_REAL_ROLE"));

        assertThatThrownBy(() -> service.createUser(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // Positive path: valid request persists a hashed password and the resolved roles.
    @Test
    void createUser_withValidRequest_persistsHashedPasswordAndRoles() {
        when(userRepository.existsByUsername("jsmith")).thenReturn(false);
        when(userRepository.existsByEmail("j@example.com")).thenReturn(false);
        when(roleRepository.findByRoleCode("COSTING_ENGINEER")).thenReturn(Optional.of(costingEngineerRole()));
        when(passwordEncoder.encode("ValidPass1!")).thenReturn("hashed-value");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateUserRequest request = new CreateUserRequest("jsmith", "j@example.com",
                "ValidPass1!", Set.of("COSTING_ENGINEER"));
        UserResponse result = service.createUser(request);

        assertThat(result.username()).isEqualTo("jsmith");
        assertThat(result.roleCodes()).containsExactly("COSTING_ENGINEER");
        assertThat(result.status()).isEqualTo("Active");
        verify(auditService).recordCreate(eq("User"), anyString(), any());
    }

    // TC-M18 (SEC-08, Positive): deactivation flips status AND bumps tokenVersion, so
    // already-issued JWTs stop validating immediately (see JwtTokenProvider.isTokenValid).
    @Test
    void deactivate_setsStatusInactiveAndBumpsTokenVersion() {
        User user = new User();
        user.setUserId("user-1");
        user.setStatus("Active");
        user.setTokenVersion(3);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse result = service.deactivate("user-1");

        assertThat(result.status()).isEqualTo("Inactive");
        // tokenVersion isn't in the response DTO by design (it's an internal revocation
        // mechanism, not user-facing data) - assert against the entity mutation directly.
        assertThat(user.getTokenVersion()).isEqualTo(4);
    }
}
