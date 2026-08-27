package com.automotive.costing.platform.access.repository;

import com.automotive.costing.platform.access.entity.UserInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserInvitationRepository extends JpaRepository<UserInvitation, String> {
    Optional<UserInvitation> findByTokenHashAndStatus(String tokenHash, String status);
    List<UserInvitation> findByUserIdAndStatus(String userId, String status);
    Optional<UserInvitation> findByInvitationIdAndCustomerId(String invitationId, String customerId);
}
