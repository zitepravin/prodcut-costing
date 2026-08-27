package com.automotive.costing.platform.user.repository;

import com.automotive.costing.platform.user.entity.OidcIdentityLink;
import com.automotive.costing.platform.user.entity.OidcLinkStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OidcIdentityLinkRepository extends JpaRepository<OidcIdentityLink, String> {

    Optional<OidcIdentityLink> findByIssuerAndSubjectAndStatus(String issuer, String subject, OidcLinkStatus status);

    List<OidcIdentityLink> findByUserIdAndStatusOrderByLinkedAtDesc(String userId, OidcLinkStatus status);

    boolean existsByIssuerAndSubjectAndStatus(String issuer, String subject, OidcLinkStatus status);
}
