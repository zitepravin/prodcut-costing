package com.automotive.costing.billing.repository;

import com.automotive.costing.billing.entity.BillingWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BillingWebhookEventRepository extends JpaRepository<BillingWebhookEvent, String> {
    Optional<BillingWebhookEvent> findByGatewayCodeAndGatewayEventId(String gatewayCode, String gatewayEventId);
}
