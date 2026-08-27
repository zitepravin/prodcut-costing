package com.automotive.costing.platform.user.service;

import com.automotive.costing.platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deliberately a separate Spring bean from {@code AuthService}, not a private method on
 * it. {@code @Transactional(REQUIRES_NEW)} only takes effect on calls that go through
 * Spring's AOP proxy - a self-invocation (one method on a class calling another method on
 * "this") bypasses the proxy entirely and the annotation is silently ignored. Since the
 * whole point here is that a failed-login attempt must be recorded even though
 * {@code AuthService.login()}'s own transaction is about to roll back and rethrow, this
 * needs to be a genuine cross-bean call for {@code REQUIRES_NEW} to actually do anything.
 */
@Component
@RequiredArgsConstructor
public class LoginAttemptTracker {

    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedAttempt(String username) {
        userRepository.findByUsername(username).ifPresent(u -> {
            u.setFailedLoginAttempts(u.getFailedLoginAttempts() + 1);
            userRepository.save(u);
        });
    }
}
