package com.automotive.costing.vendorselection.service;

import com.automotive.costing.common.audit.AuditService;
import com.automotive.costing.common.exception.ResourceNotFoundException;
import com.automotive.costing.vendorselection.entity.VendorRecommendationDecisionEvent;
import com.automotive.costing.vendorselection.entity.VendorRecommendationEventDelivery;
import com.automotive.costing.vendorselection.repository.VendorRecommendationDecisionEventRepository;
import com.automotive.costing.vendorselection.repository.VendorRecommendationEventDeliveryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Delivers unpublished recommendation decision events to a configured webhook.
 * Delivery never changes recommendation status and never auto-approves.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationDecisionWebhookService {

    private final VendorRecommendationDecisionEventRepository eventRepository;
    private final VendorRecommendationEventDeliveryRepository deliveryRepository;
    private final VendorRecommendationService recommendationService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Value("${vendor-selection.decision-webhook.enabled:false}")
    private boolean enabled;

    @Value("${vendor-selection.decision-webhook.url:}")
    private String webhookUrl;

    @Value("${vendor-selection.decision-webhook.timeout-ms:5000}")
    private long timeoutMs;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public record DeliveryResponse(
            String deliveryId,
            String eventId,
            String channel,
            boolean success,
            Integer httpStatus,
            int attemptNo,
            String errorMessage
    ) {}

    public record DispatchSummary(int attempted, int succeeded, int failed, boolean enabled, String targetUrl) {}

    @Transactional
    public DispatchSummary dispatchUnpublished() {
        if (!enabled || webhookUrl == null || webhookUrl.isBlank()) {
            log.debug("Decision webhook disabled or URL empty — skip dispatch");
            return new DispatchSummary(0, 0, 0, enabled, webhookUrl);
        }
        List<VendorRecommendationDecisionEvent> pending =
                eventRepository.findByPublishedFlagFalseOrderByOccurredAtAsc();
        int ok = 0;
        int fail = 0;
        for (VendorRecommendationDecisionEvent event : pending) {
            DeliveryResponse r = deliverOne(event);
            if (r.success()) {
                ok++;
            } else {
                fail++;
            }
        }
        return new DispatchSummary(pending.size(), ok, fail, true, webhookUrl);
    }

    @Transactional
    public DeliveryResponse deliverEvent(String eventId) {
        VendorRecommendationDecisionEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> ResourceNotFoundException.of("VendorRecommendationDecisionEvent", eventId));
        return deliverOne(event);
    }

    private DeliveryResponse deliverOne(VendorRecommendationDecisionEvent event) {
        int attempt = (int) deliveryRepository.countByEventId(event.getEventId()) + 1;
        VendorRecommendationEventDelivery d = new VendorRecommendationEventDelivery();
        d.setDeliveryId(UUID.randomUUID().toString());
        d.setEventId(event.getEventId());
        d.setChannel("WEBHOOK");
        d.setTargetUrl(webhookUrl);
        d.setAttemptNo(attempt);
        d.setAttemptedAt(Instant.now());

        if (!enabled || webhookUrl == null || webhookUrl.isBlank()) {
            d.setSuccessFlag(false);
            d.setErrorMessage("Webhook disabled or URL not configured");
            deliveryRepository.save(d);
            return new DeliveryResponse(d.getDeliveryId(), event.getEventId(), "WEBHOOK", false, null, attempt,
                    d.getErrorMessage());
        }

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("eventId", event.getEventId());
            body.put("eventType", event.getEventType());
            body.put("recommendationId", event.getRecommendationId());
            body.put("decisionId", event.getDecisionId());
            body.put("previousStatus", event.getPreviousStatus() != null ? event.getPreviousStatus().name() : null);
            body.put("newStatus", event.getNewStatus() != null ? event.getNewStatus().name() : null);
            body.put("recommendedSupplierId", event.getRecommendedSupplierId());
            body.put("overrideSupplierId", event.getOverrideSupplierId());
            body.put("occurredAt", event.getOccurredAt() != null ? event.getOccurredAt().toString() : null);
            body.put("occurredBy", event.getOccurredBy());
            body.put("humanControlled", true);
            body.put("autoApproveForbidden", true);
            body.put("payload", event.getPayloadJson());

            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .header("X-Product-Costing-Event", "VendorRecommendationDecision")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            d.setHttpStatus(status);
            String excerpt = response.body();
            if (excerpt != null && excerpt.length() > 500) {
                excerpt = excerpt.substring(0, 500);
            }
            d.setResponseExcerpt(excerpt);
            boolean success = status >= 200 && status < 300;
            d.setSuccessFlag(success);
            if (!success) {
                d.setErrorMessage("HTTP " + status);
            }
            deliveryRepository.save(d);
            if (success && !event.isPublishedFlag()) {
                recommendationService.markEventPublished(event.getEventId());
            }
            auditService.recordCreate("VendorRecommendationEventDelivery", d.getDeliveryId(),
                    event.getEventId() + " HTTP " + status);
            return new DeliveryResponse(d.getDeliveryId(), event.getEventId(), "WEBHOOK", success, status, attempt,
                    d.getErrorMessage());
        } catch (Exception ex) {
            d.setSuccessFlag(false);
            d.setErrorMessage(ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
            deliveryRepository.save(d);
            log.warn("Webhook delivery failed for event {}: {}", event.getEventId(), d.getErrorMessage());
            return new DeliveryResponse(d.getDeliveryId(), event.getEventId(), "WEBHOOK", false, null, attempt,
                    d.getErrorMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<DeliveryResponse> listDeliveries(String eventId) {
        return deliveryRepository.findByEventIdOrderByAttemptedAtDesc(eventId).stream()
                .map(d -> new DeliveryResponse(
                        d.getDeliveryId(), d.getEventId(), d.getChannel(), d.isSuccessFlag(),
                        d.getHttpStatus(), d.getAttemptNo(), d.getErrorMessage()))
                .toList();
    }
}
