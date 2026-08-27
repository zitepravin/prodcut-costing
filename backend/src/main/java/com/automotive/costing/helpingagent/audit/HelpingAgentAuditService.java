package com.automotive.costing.helpingagent.audit;

import com.automotive.costing.common.audit.AuditService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HelpingAgentAuditService {
    private static final Logger log = LoggerFactory.getLogger(HelpingAgentAuditService.class);
    private final AuditService auditService;

    public void recordRequest(String username, String customerId, String correlationId,
                              String questionPreview, List<String> tools) {
        String preview = questionPreview == null ? ""
                : questionPreview.substring(0, Math.min(120, questionPreview.length()));
        log.info("HelpingAgent request user={} customer={} correlationId={} tools={} preview={}",
                username, customerId, correlationId, tools, preview);
        try {
            auditService.recordUpdate("HelpingAgentChat",
                    correlationId != null ? correlationId : "n/a",
                    null, "tools=" + tools + ";user=" + username + ";customer=" + customerId);
        } catch (Exception e) {
            log.warn("HelpingAgent audit write failed: {}", e.getMessage());
        }
    }
}
